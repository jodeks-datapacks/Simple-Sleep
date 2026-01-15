# Simple Sleep

<div style="display: flex; gap: 20px;">
<a href="https://ko-fi.com/H2H011LYHJ"><img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="ko-fi"/></a>

<a href="https://github.com/sponsors/Jodekq" target="_blank">
  <img src="https://img.shields.io/badge/Sponsor-Jodek-green?style=for-the-badge&logo=github&logoWidth=20" alt="Sponsor @Jodek" />
</a>
</div>

💡 **Tip:** Questions or issues? -> [discord server](https://discord.gg/z2n3qTzQY6) | _or create an issue on github_

## Features

- Config to either use amount or percentage of players that must sleep to skip the night
- Displays how many players are sleeping

## Config

Location: `config/SimpleSleep.json`

```
{
"mode": "percentage", --> either "percentage" or "amount"
"percentageRequired": 0.5, --> decimal value, e.g. 0.5 = 50% (default)
"amountRequired": 3, --> int value, e.g. 3 (default)
"showSleepingPlayers": true, --> either true or false
"_comment": "Mode can be 'percentage' or 'amount'. If 'percentage', uses percentageRequired (0.0-1.0). If 'amount', uses amountRequired."
}
```