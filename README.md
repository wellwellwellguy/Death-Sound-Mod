# Death Sound Mod                                        
                                     
This mod enhances your Minecraft experience with sounds on player deaths with makes deaths more dramatic (great for SMP's).

---

## Features

* **Death Audio:** Plays a customizable sound whenever a player dies.
* **Detailed Chat Notifications:** Receive precise coordinates and the distance to the death location.
* **Visual Effects:** Optional thunder/lightning effects to mark a player's passing.
* **Totem Monitoring:** Track when players pop a Totem of Undying, including their location and distance from you.
* **Custom Sound Support:** Add your own .ogg files to the mod's folder and use them in-game.
* **Death History & Stats:** Keep track of who dies the most and view a log of recent deaths.
* **Flexible Filtering:** Choose to hear everyone, only specific friends, or exclude yourself.
* **Multi-Language Support:** Includes native support for English, German, Filipino, Bavarian, Austrian and Pirate Speech.

---

## Commands

Most of these commands are available in the menu aswell.

| Command | Description |
| :--- | :--- |
| /deathsound on | Enables the mod entirely. |
| /deathsound off | Disables the mod entirely. |
| /deathsound reset | Resets all settings to their factory default. |
| /deathsound status | Displays a detailed overview of your current settings in chat. |
| /deathsound volume <0-100> | Sets the volume of the death sounds. |
| /deathsound pitch <0.1-2.0> | Adjusts the pitch of the sound (1.0 is default). |
| /deathsound set-sound <name> | Changes the custom sound effect played upon death. |
| /deathsound custom-deathsound <on/off> | Enables or disables the use of custom .ogg sounds. |
| /deathsound test | Plays the current sound and effect to test your settings. |
| /deathsound test-effect | Spawns a thunder |
| /deathsound filter <all/others/list> | Set filter to everyone, everyone except you, or your list only. |
| /deathsound distance <chunks/unlimited> | Limits sounds to deaths within a certain chunk radius. |
| /deathsound monitor deaths <on/off> | Toggles whether death coordinates are printed in chat. |
| /deathsound monitor pops <on/off> | Toggles notifications for Totem of Undying usage. |
| /deathsound unit <meters/feet> | Switches between Metric and Imperial units for distance. |
| /deathsound history | Shows the last few deaths recorded during your session. |
| /deathsound stats | Shows a leaderboard of Top Deaths and Top Totem Pops. |
| /deathsound locate <player> | Displays the last known death coordinates of a specific player. |

---

## Custom Sounds

To use your own deathsounds:

1. Navigate to your Minecraft folder: `.minecraft/resourcepacks/DeathSound-Resources/assets/deathsound/sounds/` or klick the "Open Sounds Folder button in the deathsound menu
2. Drop your `.ogg` files there.
3. Enable custom sounds in the menu and then select your sound there or with `/deathsound set-sound <filename>`.

Note: Ensure your files are in .ogg format; other formats like .mp3 or .wav are not supported by the Minecraft engine natively.
