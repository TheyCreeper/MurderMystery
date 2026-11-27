# MurderMystery

## Parties

- `/party invite <player>` creates a party if needed and invites an online player.
- `/party accept <leader>` or `/party decline <leader>` responds to an invitation.
- `/party list` shows the leader and members; `/party leave` leaves the party.
- The leader can use `/party kick <player>` and `/party disband`.

Only the leader can invite players or start a party game with `/mmstart <map>`.
All current party members join; without a party, only the player starting the
game joins. The existing permission for `/mmstart` still applies.
Stopping a game with `/mmstop` restores participants' saved inventories and
game modes.

Detectives and innocents can shoot the murderer with a bow to win. Shooting an
innocent or detective eliminates both the shooter and their teammate.

Players leave their parties when they disconnect. If the leader leaves or
disconnects, leadership passes to the longest-standing remaining member and
pending invitations are cancelled. Parties and invitations are not saved
across plugin restarts. Party changes after a game starts do not change that
game's participants.
