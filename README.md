# Job Execution Service
Service for execution of Narrative Methods and Apps as well as async SDK jobs in docker container on AWE workers (it also wraps Narrative Job Service API).

## To run tests:
1. copy `test.cfg.example` to `test.cfg` and fill it in.
2. Install [go](http://golang.org). Note that currently AWE will not compile
   with go 1.6, so the go version must be 1.5.X or lower.
3. make
4. make test