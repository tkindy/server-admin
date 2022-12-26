# server-admin

Tool for deploying new versions of some of my personal projects.
Namely:

- [synchro](https://github.com/tkindy/synchro)
- [jeopardy](https://github.com/tkindy/jeopardy)

These projects are both single services that simply require reverse-proxying.
That's all this tool supports right now, but I hope to expand it to cover my other deploy needs.

This tool:

- picks a free port,
- starts up a new instance of the service in a [screen](https://linux.die.net/man/1/screen) session,
- waits for it to become healthy (responds with a 200 to a request for the home page),
- updates the [Caddy](https://caddyserver.com/) configuration to reverse-proxy to the new instance, and
- shuts down the old instance.
