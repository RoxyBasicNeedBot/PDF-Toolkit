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

def get_app_version():
    vname, vcode = "1.0.0", 1
    env_ver = os.environ.get("VERSION") or os.environ.get("APP_VERSION_NAME")
    if env_ver:
        vname = env_ver.strip().lstrip("v")
    env_code = os.environ.get("VERSION_CODE") or os.environ.get("APP_VERSION_CODE")
    if env_code and env_code.isdigit():
        vcode = int(env_code)
    return vname, vcode

def get_arch_label(filename: str) -> str:
    fn = filename.lower()
    if "arm64" in fn:
        return "ARM64 (64-bit - Recommended for modern devices)"
    elif "armeabi" in fn:
        return "ARMv7 (32-bit - Older devices)"
    elif "x86_64" in fn:
        return "x86_64 (Emulators / Chromebooks / PC)"
    elif "universal" in fn:
        return "Universal (All Architectures)"
    return "Universal"

def build_caption(apk_path: Path, version_name: str, version_code: int, git_sha: str, git_msg: str) -> str:
    arch = get_arch_label(apk_path.name)
    size_mb = apk_path.stat().st_size / (1024 * 1024)
    return (
        f"📄 <b>PDF Toolkit</b> <code>v{version_name}</code>\n"
        f"📦 Architecture: <b>{arch}</b>\n"
        f"📊 Size: <code>{size_mb:.2f} MB</code> | Build: <code>{version_code}</code>\n"
        f"🔗 Commit: <code>{git_sha}</code>\n"
        f"💬 Highlights: {git_msg[:120]}\n\n"
        f"🔒 100% Offline &amp; Private PDF Operations\n"
        f"#pdftoolkit #android #{apk_path.stem.split('-')[-1]}"
    )

async def upload_all_pyrogram(api_id, api_hash, bot_token, owner_ids, apk_files, topic_id, version_name, version_code, git_sha, git_msg):
    from pyrogram import Client
    print(f"=== 🚀 Initializing Pyrogram MTProto 2.0 to upload {len(apk_files)} APKs ===")
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
        thread_id = int(topic_id) if topic_id and topic_id.isdigit() else None

        for idx, apk in enumerate(apk_files, 1):
            caption = build_caption(apk, version_name, version_code, git_sha, git_msg)
            size_mb = apk.stat().st_size / (1024 * 1024)
            print(f"[{idx}/{len(apk_files)}] Uploading {apk.name} ({size_mb:.2f} MB) to chat {first_chat_id}...")

            kwargs = {
                "chat_id": first_chat_id,
                "document": str(apk),
                "caption": caption,
                "file_name": apk.name
            }
            if thread_id:
                kwargs["message_thread_id"] = thread_id

            msg = await app.send_document(**kwargs)
            file_id = msg.document.file_id
            print(f"✅ Uploaded {apk.name}! file_id: {file_id}")

            # Send to remaining owner IDs using cached file_id
            for other_cid in owner_ids[1:]:
                try:
                    cid = int(other_cid)
                    other_kwargs = {
                        "chat_id": cid,
                        "document": file_id,
                        "caption": caption,
                        "file_name": apk.name
                    }
                    if thread_id:
                        other_kwargs["message_thread_id"] = thread_id
                    await app.send_document(**other_kwargs)
                    print(f"  ↪ Delivered to chat {cid}")
                except Exception as ex:
                    print(f"  ⚠️ Delivery note for {other_cid}: {ex}")

def send_telegram_document_http(bot_token, chat_id, file_path: Path, caption: str, topic_id: str = None):
    file_size = file_path.stat().st_size
    boundary = uuid.uuid4().hex
    parts = []
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="chat_id"\r\n\r\n{chat_id}\r\n'.encode('utf-8'))
    if topic_id:
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="message_thread_id"\r\n\r\n{topic_id}\r\n'.encode('utf-8'))
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
        return data.get("ok", False)

def upload_all_http(bot_token, owner_ids, apk_files, topic_id, version_name, version_code, git_sha, git_msg):
    print(f"=== 🌐 Using Telegram Bot HTTP API to upload {len(apk_files)} APKs ===")
    for idx, apk in enumerate(apk_files, 1):
        caption = build_caption(apk, version_name, version_code, git_sha, git_msg)
        size_mb = apk.stat().st_size / (1024 * 1024)
        print(f"[{idx}/{len(apk_files)}] Sending {apk.name} ({size_mb:.2f} MB)...")
        for chat_id in owner_ids:
            try:
                ok = send_telegram_document_http(bot_token, chat_id, apk, caption, topic_id)
                if ok:
                    print(f"  ✅ Sent {apk.name} to {chat_id}")
                else:
                    print(f"  ⚠️ HTTP upload returned non-ok for {chat_id}")
            except Exception as e:
                print(f"  ❌ Failed to send {apk.name} to {chat_id}: {e}")

def main():
    bot_token = os.environ.get("BOT_TOKEN")
    api_id = os.environ.get("API_ID")
    api_hash = os.environ.get("API_HASH")
    owner_ids_raw = os.environ.get("OWNER_CHAT_ID") or os.environ.get("CHAT_ID", "")
    owner_ids = [cid.strip() for cid in owner_ids_raw.split() if cid.strip()]
    topic_id = os.environ.get("TOPIC_ID")

    if not bot_token or not owner_ids:
        print("Notice: BOT_TOKEN or OWNER_CHAT_ID environment variables are not set. Skipping Telegram upload.")
        return

    # Scan for all release APKs across standard output directories
    search_dirs = [
        Path("dist"),
        Path("androidApp/build/outputs/apk")
    ]

    found_apks = {}
    for d in search_dirs:
        if d.exists():
            for apk in d.rglob("*.apk"):
                # Exclude unaligned or intermediate APKs if any
                if "unaligned" not in apk.name.lower():
                    found_apks[apk.name] = apk

    if not found_apks:
        print("No release APK files found to upload.")
        return

    # Priority sorting: arm64-v8a first, then universal, then armeabi-v7a, then x86_64
    def sort_key(item):
        name = item.name.lower()
        if "arm64" in name:
            return 0
        elif "universal" in name:
            return 1
        elif "armeabi" in name:
            return 2
        elif "x86_64" in name:
            return 3
        return 4

    apk_files = sorted(found_apks.values(), key=sort_key)
    print(f"Discovered {len(apk_files)} APKs for Telegram upload:")
    for a in apk_files:
        print(f" - {a.name} ({a.stat().st_size / (1024*1024):.2f} MB)")

    git_sha, git_msg = get_git_info()
    version_name, version_code = get_app_version()

    uploaded_via_mtproto = False
    if api_id and api_hash:
        try:
            import pyrogram
            asyncio.run(upload_all_pyrogram(
                api_id, api_hash, bot_token, owner_ids, apk_files, topic_id,
                version_name, version_code, git_sha, git_msg
            ))
            uploaded_via_mtproto = True
        except Exception as e:
            print(f"Pyrogram MTProto error: {e}. Falling back to HTTP Bot API...")

    if not uploaded_via_mtproto:
        upload_all_http(
            bot_token, owner_ids, apk_files, topic_id,
            version_name, version_code, git_sha, git_msg
        )

    print("🎉 All APK deliveries to Telegram completed successfully!")

if __name__ == "__main__":
    main()
