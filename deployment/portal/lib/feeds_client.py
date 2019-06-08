import requests
import json
import logging
import datetime
from .utils import send_slack_message
import os
import sys


class feeds_client:
    def __init__(self, username, token, uri):
        self.api_version = "api/V1"
        self.admin_username = username
        self.admin_token = token
        self.service_url = uri
        logging.basicConfig(level=logging.INFO)

    def get_notifications(self):
        url = f"{self.service_url}/{self.api_version}/notifications"
        logging.info("About to send [notifications] request to " + url)
        return requests.get(url, timeout=1, headers={"Authorization": self.admin_token})

    def get_permissions(self):
        url = f"{self.service_url}/permissions"
        logging.info("About to send [permissions]request to " + url)
        return requests.get(url, timeout=1, headers={"Authorization": self.admin_token})

    def notify_users_workspace(
        self, user, message, job_id, app_name, alert_level="error", dryRun=True
    ):
        """
        This is a hack as the job object is not working

        """
        note = {
            "actor": {"id": self.admin_username, "type": "admin"},
            "source": "jobsservice",
            "verb": "update",
            "level": alert_level,
            # "object": {
            #     "id": job_id",
            #     "type": "job",
            #     "name": "App Name"
            # },
            "object": {"id": job_id, "type": "workspace", "name": app_name},
            "target": [{"id": user, "type": "user"}],
            "users": [{"id": user, "type": "user"}],
            "context": {"text": message},
        }
        url = f"{self.service_url}/{self.api_version}/notification"
        logging.info("About to send [message]request to " + url)

        if dryRun is False:
            return requests.post(
                url, json=note, headers={"Authorization": self.admin_token}
            )
        else:
            logging.INFO(url)
            logging.INFO(note)


    #TODO How do you annotate this?
    @classmethod
    def feeds_service_client(cls):
        service_token = os.environ.get("FEEDS_SERVICE_TOKEN")
        service_url = os.environ.get("FEEDS_ENDPOINT")

        if service_token is None or service_url is None:
            sys.exit("FAILURE: Please set FEEDS_SERVICE_TOKEN and FEEDS_ENDPOINT")

        feed_service = feeds_client("kbase", service_token, service_url)

        return feed_service


if __name__ == "__main__":
    fc = feeds_client.feeds_service_client()
    message = "Test"
    fc.notify_users_workspace(
        user="bsadkhin", message=message, job_id="jobId", app_name="App_name"
    )

    help(fc.notify_users_workspace)
    send_slack_message(message)
