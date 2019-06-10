import datetime

from mongoengine import *


class Line(EmbeddedDocument):
    line = StringField()
    line_pos = IntField()
    is_error = BooleanField()


class Log(Document):
    ujs_job_id = StringField(required=True)
    original_line_count = IntField()
    stored_line_count = IntField()
    lines = EmbeddedDocumentField(Line, default=[])


connect('exec_engine')


line = Line(line="This is a log", line_pos=0, is_error=True)
log = Log(ujs_job_id="ujs_id", original_line_count=1, stored_line_count=1, lines=line)

#TODO Mimic log in EXECENGINEDB?