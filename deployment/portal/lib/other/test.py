import dryable
import requests
import sys


def findOldFilesForDeletion():
    return ["some", "results"]


@dryable.Dryable()
def deleteFiles(results):
    print(
        "will now open an real world connection"
        "that requires a server and will make side effects"
    )
    requests.post("http://url.to.some.server/results", data=str(results))


sys.argv.append("--dry-run")

# the next line ensures that deleteFiles
# will not run if --dry-run is specified on the command line
dryable.set("--dry-run" in sys.argv)

# now code as usual
results = findOldFilesForDeletion()
print("got: {}".format(results))
deleteFiles(results)
