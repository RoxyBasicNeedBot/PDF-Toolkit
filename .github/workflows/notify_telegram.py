#!/usr/bin/env python3
import os
import subprocess
import requests
import html

def get_git_commit_info():
    try:
        commit_author = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%an']).decode('utf-8').strip()
        commit_message = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%s']).decode('utf-8').strip()
        commit_hash = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%H']).decode('utf-8').strip()
        commit_hash_short = subprocess.check_output(['git', 'log', '-1', '--pretty=format:%h']).decode('utf-8').strip()
        return commit_author, commit_message, commit_hash, commit_hash_short
    except Exception:
        return "Unknown", "Automated CI Build", "latest", "latest"

def main():
    bot_token = os.environ.get('BOT_TOKEN')
    chat_ids_raw = os.environ.get('CHAT_ID')
    if not bot_token or not chat_ids_raw:
        print("Notice: BOT_TOKEN or CHAT_ID environment variables are not set. Skipping Telegram notification.")
        return
    topic_id = os.environ.get('TOPIC_ID')
    repo = os.environ.get('GITHUB_REPOSITORY', 'RoxyBasicNeedBot/PDF-Toolkit')

    commit_author, commit_message, commit_hash, commit_hash_short = get_git_commit_info()

    safe_author = html.escape(commit_author)
    safe_message = html.escape(commit_message)
    commit_url = f"https://github.com/{repo}/commit/{commit_hash}"

    message = (
        f"📄 <b>Roxy PDF Toolkit CI / CD Build Started</b>\n\n"
        f"A new <a href=\"{commit_url}\">commit</a> has been pushed by <b>{safe_author}</b>:\n"
        f"<blockquote>{safe_message}</blockquote>\n\n"
        f"Building with <b>Kotlin Compose Multiplatform</b>, <b>R8 Code &amp; Resource Shrinking</b> &amp; <b>Liquid Glass Architecture</b>.\n\n"
        f"#{commit_hash_short}"
    )

    chat_ids = [cid.strip() for cid in chat_ids_raw.split() if cid.strip()]

    for chat_id in chat_ids:
        try:
            url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
            payload = {
                "chat_id": chat_id,
                "text": message,
                "parse_mode": "HTML",
                "disable_web_page_preview": True
            }
            if topic_id:
                payload["message_thread_id"] = topic_id

            response = requests.post(url, json=payload, timeout=15)
            if response.status_code != 200:
                print(f"Failed to send start message to {chat_id}: {response.status_code} {response.text}")
            else:
                print(f"Start message sent successfully to {chat_id}.")
        except Exception as e:
            print(f"Error sending start message to {chat_id}: {e}")

if __name__ == "__main__":
    main()
