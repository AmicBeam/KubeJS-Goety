ServerEvents.loaded(event => event.server.scheduleInTicks(120, () => event.server.runCommandSilent('stop')))
