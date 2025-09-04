### Attributes a player has
- from the Plugin
  - time left
  - mode:
    - ticking: normal behaviour, clock ticking
    - pause: clock not ticking
    - bypass: clock ticking but no punishment given
- by permission
  - group (specifies the applied rules)

### Groups & Rules
```yaml
default-group: testgroup
groups:
  testgroup: # This name is custom and will lead to a permission `limiter.group.testgroup`
    start-timer: 5:00 # (default is 5:00)
    start-mode: ticking # (default is ticking)
    rules:
      daily: # This rules name is custom and must be unique for this group
        when:
          time: 0:00
        action:
          change-timer: +5:00
      night:
        when:
          time: 21:00
        action:
          restrict: deny
      morning:
        when:
          time: 7:00
        action:
          restrict: allow
    on-expiration:
      command: tellraw "Time's up!"
```

#### `action`s:
- set-timer
- change-timer
- restrict
- kick
- ban
- command

#### `when`s:
- time
- online (bool): whether the player has to be online
