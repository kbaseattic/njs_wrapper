import os
import requests
import sys
import json

if (os.environ.get("SLACK_WEBHOOK_URL") is None):
    sys.exit("FAILURE: Webhook URL is not available")

def send_slack_message(message):
    """
    :param message: Escaped Message to send to slack
    """
    # ee_notifications_channel
    webhook_url = os.environ.get("SLACK_WEBHOOK_URL")
    if(webhook_url is None):
        sys.exit("FAILURE: Webhook URL is not available")
    slack_data = {'text': message}
    requests.post(
        webhook_url, data=json.dumps(slack_data),
        headers={'Content-Type': 'application/json'}
    )