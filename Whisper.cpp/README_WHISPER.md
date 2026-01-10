**STEPS to run the STT model on your system**
Steps:
1.Make sure you have 2 files : Whisper_CLI.exe and the model of your choice for the system
2.Then open your command prompt/CMD
3.Write this commands : setx WHISPER_CLI "your  .exe file path" and setx WHISPER_MODEL "your model file path"
4.after executing this commands make sure to restart the terminal i.e to close the CMD and start it again
5.After the CMD opens for verification your paths are set or not run these commands:
echo %WHISPER_CLI%
echo %WHISPER_MODEL%
6.Then run the STT file 