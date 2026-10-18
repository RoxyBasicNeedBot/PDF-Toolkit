#!/usr/bin/env python3
import os
import sys
import json
import uuid
import asyncio
import urllib.request
import subprocess
from pathlib import Path

def get_git_info():
    try:
        sha = subprocess.check_output(["git", "rev-parse", "--short", "HEAD"]).decode().strip()
        msg = subprocess.check_output(["git", "log", "-1", "--pretty=%B"]).decode().strip()
        return sha, msg
    except Exception:
        return "latest", "Automated release build"

async def upload_with_pyrogram(api_id, api_hash, bot_token, owner_ids, file_path, caption):
    from pyrogram import Client
    print("=== 🚀 Initializing Pyrogram MTProto 2.0 Client ===")
    app = Client(
        "roxy_bot_uploader",
        api_id=int(api_id),
        api_hash=api_hash,
        bot_token=bot_token,
        in_memory=True
    )

    async with app:
        me = await app.get_me()
        print(f"Connected to Telegram as @{me.username} (ID: {me.id})")

        first_chat_id = int(owner_ids[0])
        print(f"Uploading {file_path.name} ({file_path.stat().st_size / (1024*1024):.2f} MB) to chat {first_chat_id}...")

        msg = await app.send_document(
            chat_id=first_chat_id,
            document=str(file_path),
            caption=caption,
            file_name=file_path.name
        )

        file_id = msg.document.file_id
        file_size = msg.document.file_size
        message_id = msg.id
        print(f"Successfully uploaded to Telegram! file_id: {file_id}, message_id: {message_id}")

        for chat_id in owner_ids[1:]:
            try:
                cid = int(chat_id)
                print(f"Delivering APK to owner ID {cid} using file_id...")
                await app.send_document(
                    chat_id=cid,
                    document=file_id,
                    caption=caption,
                    file_name=file_path.name
                )
            except Exception as ex:
                print(f"Delivery note for {chat_id}: {ex}")

        return {
            "file_id": file_id,
            "file_name": file_path.name,
            "file_size": file_size,
            "message_id": message_id,
            "chat_id": first_chat_id
        }

def send_telegram_document_http(bot_token, chat_id, file_path, caption):
    file_path = Path(file_path)
    file_size = file_path.stat().st_size
    print(f"Fallback HTTP upload: {file_path.name} ({file_size / (1024*1024):.2f} MB) to {chat_id}...")
    boundary = uuid.uuid4().hex
    parts = []
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="chat_id"\r\n\r\n{chat_id}\r\n'.encode('utf-8'))
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="caption"\r\n\r\n{caption}\r\n'.encode('utf-8'))
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="parse_mode"\r\n\r\nHTML\r\n'.encode('utf-8'))
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="document"; filename="{file_path.name}"\r\nContent-Type: application/vnd.android.package-archive\r\n\r\n'.encode('utf-8'))
    with open(file_path, 'rb') as f:
        parts.append(f.read())
    parts.append(f'\r\n--{boundary}--\r\n'.encode('utf-8'))
    req = urllib.request.Request(
        f"https://api.telegram.org/bot{bot_token}/sendDocument",
        data=b''.join(parts),
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"}
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        if data.get("ok"):
            doc = data["result"].get("document", {})
            return {
                "file_id": doc.get("file_id"),
                "file_name": file_path.name,
                "file_size": file_size,
                "message_id": data["result"].get("message_id"),
                "chat_id": chat_id
            }
    return None

def get_app_version():
    vname, vcode = "1.0.0", 1
    env_ver = os.environ.get("VERSION") or os.environ.get("APP_VERSION_NAME")
    if env_ver:
        vname = env_ver.strip().lstrip("v")
    env_code = os.environ.get("VERSION_CODE") or os.environ.get("APP_VERSION_CODE")
    if env_code and env_code.isdigit():
        vcode = int(env_code)

    return vname, vcode

def main():
    bot_token = os.environ.get("BOT_TOKEN")
    api_id = os.environ.get("API_ID")
    api_hash = os.environ.get("API_HASH")
    owner_ids_raw = os.environ.get("OWNER_CHAT_ID") or os.environ.get("CHAT_ID", "")
    owner_ids = [cid.strip() for cid in owner_ids_raw.split() if cid.strip()]

    if not bot_token or not owner_ids:
        print("Notice: BOT_TOKEN or OWNER_CHAT_ID environment variables are not set. Skipping Telegram upload.")
        return

    apk_dirs = [
        Path("dist"),
        Path("androidApp/build/outputs/apk/universal/release"),
        Path("androidApp/build/outputs/apk/release"),
        Path("androidApp/build/outputs/apk/debug")
    ]
    apk_dir = None
    for d in apk_dirs:
        if d.exists() and list(d.glob("*.apk")):
            apk_dir = d
            break

    if not apk_dir:
        print("No APK directory with APK files found.")
        return

    # Prioritize universal or arm64 APKs
    target_apk = None
    for pattern in ["*universal*.apk", "*arm64*.apk", "*.apk"]:
        matches = list(apk_dir.glob(pattern))
        if matches:
            target_apk = matches[0]
            break

    if not target_apk:
        print("No APK found to upload.")
        return

    git_sha, git_msg = get_git_info()
    version_name, version_code = get_app_version()

    caption = (
        f"📄 <b>Roxy PDF Toolkit New Release</b>\n"
        f"Version: <code>v{version_name}</code> (Build {version_code})\n"
        f"Commit: <code>{git_sha}</code>\n"
        f"Highlights: {git_msg[:200]}\n\n"
        f"🔒 100% Offline &amp; Private PDF Operations\n"
        f"#roxypdftoolkit #android #release"
    )

    upload_res = None
    if api_id and api_hash:
        try:
            import pyrogram
            upload_res = asyncio.run(upload_with_pyrogram(api_id, api_hash, bot_token, owner_ids, target_apk, caption))
        except Exception as e:
            print(f"Pyrogram MTProto upload note: {e}. Falling back to HTTP Bot API...")

    if not upload_res:
        for chat_id in owner_ids:
            try:
                res = send_telegram_document_http(bot_token, chat_id, target_apk, caption)
                if res and not upload_res:
                    upload_res = res
            except Exception as he:
                print(f"HTTP upload to {chat_id} error: {he}")

    print(f"Telegram processing completed for {target_apk.name}.")

if __name__ == "__main__":
    main()
