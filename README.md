FinalWords
==========

This is a plugin that helps avoid spoilers from players after they have died.

# How it works

- A player dies.
- They have (default) 60 seconds before they are banned, or until they leave the server.

![Death](/images/death.png)

- They are able to type up to (default) 3 messages in chat after which all messages are lost. Typing 'done' will cancel
early. e.g. `gg` then `done` will only be a single message (`gg`)
- These messages are then presented to admins like this:

![Admin View](/images/admin-view.png)

- Admins can then click on `Approve` or `Deny`. Denying will tell all admins + the player that their words were denied.
Accepting shows all online players the messages:

![Final](/images/final.png)

- Waiting for (default) 120 seconds without approving or denying will cause the messages to be automatically denied.

# Commands

## `/finalwords accept|deny <uuid>`

This is a command used internally for the chat click events. Requires `uhc.finalwords.admin`

# Permissions

## `uhc.finalwords.admin`

- Default OP
- Receive + Accept/Deny message requests

## `uhc.finalwords.bypass`

- Default OP
- Bypass ban/message system on death, nothing happens