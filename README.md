# MurderMystery

## Parties

- `/party invite <player>` creates a party if needed and invites an online player.
- `/party <player> [player...]` invites one or more online players. `/p` is an alias for `/party`.
- `/party accept <leader>` or `/party decline <leader>` responds to an invitation.
- `/party list` shows the leader and members; `/party leave` leaves the party.
- The leader can use `/party kick <player>` and `/party disband`.

Only the leader can invite players or start a party game with `/mmstart <map>`.
Party subcommands take precedence over player names; use `/party invite <player>`
to invite a player whose name matches a subcommand.
All current party members join; without a party, only the player starting the
game joins. The existing permission for `/mmstart` still applies.
Stopping a game with `/mmstop` restores participants' saved inventories, game
modes, and locations. Players who disconnect during a game are restored too.
At the start of a game, players are teleported to randomly selected safe
locations inside the map with solid blocks overhead. Covered outdoor areas can
also qualify. The game cannot start if no suitable location is found.
The spawn search is bounded, so particularly large or obstructed maps may need
more accessible floor space.

Detectives and innocents can shoot the murderer with a bow to win. Shooting an
innocent or detective eliminates both the shooter and their teammate.
Detectives regain an arrow five seconds after each shot, with an action bar
countdown; innocents with bows do not regain arrows this way.

Players leave their parties when they disconnect. If the leader leaves or
disconnects, leadership passes to the longest-standing remaining member and
pending invitations are cancelled. Parties and invitations are not saved
across plugin restarts. Party changes after a game starts do not change that
game's participants.
