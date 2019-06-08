import os
import json
import requests
import time
import subprocess
import datetime
import sys


def send_slack_message(message):
    """
    :param message: Escaped Message to send to slack
    """
    # ee_notifications_channel
    webhook_url = os.environ.get("SLACK_WEBHOOK_URL", sys.exit("No Slack webhook url"))
    slack_data = {"text": message}
    requests.post(
        webhook_url,
        data=json.dumps(slack_data),
        headers={"Content-Type": "application/json"},
    )


def fail():
    now = str(datetime.datetime.now())
    send_slack_message(
        "Couldn't check condor_q either due to timeout or command failure " + now
    )


def succeed(time):
    now = str(datetime.datetime.now())
    send_slack_message("Successfully ran condor_q in  " + str(time) + " seconds " + now)


while True:
    start = time.time()
    p = subprocess.check_call("condor_q > /dev/null", shell=True)
    if p == 0:
        elapsed = time.time() - start
    #    succeed(elapsed)
    else:
        fail()
    time.sleep(600)
