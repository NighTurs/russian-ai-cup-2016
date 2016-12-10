import sys

print('new StringBuilder()')
with open(sys.argv[1]) as f:
    while True:
        s = f.read(10000)
        if not s:
            break
        print('.append("' + s + '")')