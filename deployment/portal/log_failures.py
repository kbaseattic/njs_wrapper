#!/usr/bin/env python
from lib.njs_jobs import njs_jobs


if __name__ == "__main__":
    m = njs_jobs()
    m.purge_dead_jobs()
