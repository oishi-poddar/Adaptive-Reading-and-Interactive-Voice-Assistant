# chatbot-watson-android
 An Android app with an interactive reading assistant for the visually impaired
 
A.R.I.V.A:Adaptive Reading and Interactive Voice Assistant; it is an application which can QR-scan or import a document - both structured and unstructured - and then read the document back to the user, with unique options for interactivity. Users can pause, play, rewind and skip forward through the document, in addition to performing search queries (such as ‘search for my Amazon purchases) and data analysis (‘what did I spend on Amazon purchases this month?) within the file. A.R.I.V.A has additional support functions, such as saving and retrieving already-uploaded documents, or taking a user directly into a customer support phone call when requested - and is secured using a voice authentication system.

In terms of platforms/ APIs/ other tools, we used:
Google Cloud Storage: store files being used by A.R.I.V.A
Google Cloud SQL: database storage and management
IBM Watson Assistant: we created intents to manage dialogueflow, and develop an interactive voice assistant.
Google text to speech and speech to text
Microsoft Cognitive Services: using speaker verification to implement voice authentication.
Google Cloud API:  To connect our Java application with Google cloud SQL
Spring Cloud: To connect our Java application with Google cloud SQL
Google Cloud Functions:  To host Python code which reads a PDF document
We coded in Python, Java and Kotlin.

Learn how easy it is to build an IBM Watson powered ChatBot running on Android and using Watson Assistant (formerly Conversation),Text-to-Speech, Speech-to-Text Services on IBM Cloud (an open standards, cloud platform for building, running, and managing apps and services).

<p align="center"><img src="images/architecture.png"/></p>

For **step-by-step instructions**, refer this tutorial - [Build a voice-enabled Android chatbot](https://cloud.ibm.com/docs/tutorials?topic=solution-tutorials-android-watson-chatbot)

The tutorial walks you through the process of defining intents and entities and building a dialog flow for your chatbot to respond to customer queries. You will learn how to enable Speech to Text and Text to Speech services for easy interaction with the Android app.

<p align="center"><img src="images/android_chatbot.png" width="350" /><img src="images/android_watson_chatbot.png" width="350" /></p>

## Solution Tutorials 
For other solution tutorials, [click here](http://ibm.biz/solution-tutorials)
## License

See [License.txt](https://github.com/IBM-Cloud/chatbot-watson-android/blob/master/License.txt) for license information.

