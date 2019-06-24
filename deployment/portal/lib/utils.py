import os
import requests
import sys
import json
import time


def send_slack_message(message):
    if os.environ.get("SLACK_WEBHOOK_URL") is None:
        sys.exit("FAILURE: Webhook URL (SLACK_WEBHOOK_URL) is not available")

    """
    :param message: Escaped Message to send to slack
    """
    if len(message) > 15000:
        return _send_slack_message_chunks(message)

    # ee_notifications_channel
    webhook_url = os.environ.get("SLACK_WEBHOOK_URL")
    slack_data = {"text": message}
    requests.post(
        webhook_url,
        data=json.dumps(slack_data),
        headers={"Content-Type": "application/json"},
    )


def _send_slack_message_chunks(message):

    window = 15000

    for m in [message[i : i + window] for i in range(0, len(message), window)]:
        time.sleep(1)
        webhook_url = os.environ.get("SLACK_WEBHOOK_URL")
        slack_data = {"text": m}
        requests.post(
            webhook_url,
            data=json.dumps(slack_data),
            headers={"Content-Type": "application/json"},
        )
