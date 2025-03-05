# MyGPTDicto

**MyGPTDicto** is a Spring Boot application that leverages the LangChain and Gemini Pro API to read CSV files and answer questions based on their content. It’s designed to provide an intelligent, conversational way to query data stored in CSV format.

## Features
- Upload and process CSV files
- Ask natural language questions about the CSV data
- Get accurate, AI-powered responses using the LangChain and Gemini Pro API

## Prerequisites
Before running the project, ensure you have:
- **Java 17** or higher installed
- **Maven** for dependency management
- A valid **Gemini Pro API key** (obtain it from [their official site](https://ai.google.dev/gemini-api/docs/api-key))

## Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/harshbargude/mygptdicto.git
2. **Navigate to the project directory**:
   ```bash
   cd mygptdicto
3. **Configure the API key**:
  Open src/main/resources/application.properties and add your Gemini Pro API key:
    ```bash
    google.api.key=YOUR_API_KEY_HERE
4. **Build the project**:
    ```bash
    mvn clean install
5. **Run the application**:
    ```bash
    mvn spring-boot:run


## **Technologies Used**
- Spring Boot: Backend framework for building the application
- Gemini Pro API: For natural language processing and answering questions
- Java: Core programming language
- Maven: Dependency management

### Contact
For questions or feedback, reach out to me at harshbargude03@gmail.com or connect on [@HBargude](https://x.com/HBargude).
