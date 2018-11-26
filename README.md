Telegram Message Analysis Bot
===================================

This is a Telegram bot server which provides some basic message statistics and analysis functions.<br>
It interacts with users in a command-line-like way. Messages starting with '/' or '>' are considered as command.<br>

For example, if you haven't viewed a chat for long time and found there're 1000 unread messages, probably you just want to get a summary instead of viewing the messages one by one. Now you can try the 'keyword' command. Send:
<blockquote>
  >keyword -n 1000
</blockquote>
You will get a response like the following:
<blockquote>
  Keywords of latest 1000 messages<br>
  ------<br>
  11/16 03:34 EST    /goto__m_292331<br>
  food, restaurant, steak, burger, beef, chicken, sauce, fish, <br>
  ------<br>
  11/19 19:42 EST    /goto__m_295920<br>
  code, java, c++, python, programming, bug, test, release, build, <br>
  ------<br>
  ...
</blockquote>

Note: It has NOT been optimized for high QPS, but works well for limited number of chats which is configurable through a chat id whitelist in the config file.

### Functionality

Main commands:
1. <b>rank</b><br>
   Computes a rank based on the number of messages of the given type each user sent.
1. <b>timestats</b><br>
   Outputs an image to show the time distribution of chat messages.
1. <b>history</b><br>
   Outputs the history of chat titles and commands.
1. <b>keyword</b><br>
   Computes the keywords of messages in the given time range.
1. <b>network</b><br>
   Outputs an image to show the reply relationships between users.
1. <b>goto</b><br>
   Outputs a message which replies to the message you want to go to.
1. <b>chat</b><br>
   Chat with the bot.

### How to deploy

1. Clone and import this project to your machine.<br>

2. Set up database:<br>
   This project uses [MySQL](https://www.mysql.com/) database.<br>
   Please install MySQL and create a database for the bot.<br>
   To make it compatible with emoji, please set char set and collation to utf8mb4.<br>
   Fill the database URL, username and password to [config.ini](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/config.ini).<br>
   <i>If you don't want to use MySQL, you can create a new class which implements [DatabaseInterface](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/src/main/java/com/neoshell/telegram/messageanalysisbot/database/DatabaseInterface.java) and replace [MySQLDatabase](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/src/main/java/com/neoshell/telegram/messageanalysisbot/database/MySQLDatabase.java).</i><br>
  
3. Set up NLP server:<br>
   This project has dependency on [NLPUtil](https://github.com/neoshell/NLPUtil) (client) which is used for 'keyword' command.<br>
   Build the library and import it to this project, as well as set up NLPUtil server (see the README of [NLPUtil](https://github.com/neoshell/NLPUtil)).<br>
   Fill the NLPUtil server host and port to [config.ini](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/config.ini).<br>
   <i>If you don't want to use this library, you can create a new class which implements [NLPInterface](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/src/main/java/com/neoshell/telegram/messageanalysisbot/nlp/NLPInterface.java) and replace [NLPUtilClientWrapper](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/src/main/java/com/neoshell/telegram/messageanalysisbot/nlp/NLPUtilClientWrapper.java).</i><br>
   <i>If you don't want to enable 'keyword' command at all, simply remove the corresponding register statement from code.</i><br>

4. Set up Graphviz:<br>
   This project has dependency on [Graphviz](https://graphviz.gitlab.io/) which is used for generating images for 'network' command.<br>
   Please install Graphviz and fill the binary path to [config.ini](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/config.ini).<br>
   <i>If you don't want to use this library, you can create a new class which implements [GraphVisualizationInterface](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/src/main/java/com/neoshell/telegram/messageanalysisbot/visualization/GraphVisualizationInterface.java) and replace [Graphviz](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/src/main/java/com/neoshell/telegram/messageanalysisbot/visualization/Graphviz.java).</i><br>
   <i>If you don't want to enable 'network' command at all, simply remove the corresponding register statement from code.</i><br>

5. Set up Turing Robot:<br>
   This project has dependency on [Turing Robot](http://www.tuling123.com/) which is used as chat bot API for 'chat' command.<br>
   Please register Turing Robot and fill the API key to [config.ini](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/config.ini).<br>
   <i>If you don't want to use Turing Robot API, you can create a new class which implements [ChatBotInterface](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/src/main/java/com/neoshell/telegram/messageanalysisbot/chatbot/ChatBotInterface.java) and replace [TuringRobot](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/src/main/java/com/neoshell/telegram/messageanalysisbot/chatbot/TuringRobot.java).</i><br>
   <i>If you don't want to enable 'chat' command at all, simply remove the corresponding register statement from code.</i><br>

6. Build the bot as runnable jar.<br>

7. Create a new telegram bot (see the instructions [here](https://telegram.org/blog/bot-revolution)) and fill the bot name and token to [config.ini](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/config.ini).<br>

8. Fill the numeric ids of the chats you want to serve into ChatWhiteList field in [config.ini](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/config.ini).<br>

9. Fill the rest of fields in [config.ini](https://github.com/neoshell/TelegramMessageAnalysisBot/blob/master/telegram-message-analysis-bot/config.ini).<br>

10. Start the bot server.<br>

11. Set up WordCounter which computes word frequency for 'keyword' function. Schedule it to run once per month.

### How to use it in Telegram

Send:
<blockquote>
  >help
</blockquote>
It will show you help information.
