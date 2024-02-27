package com.external;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.generativeai.*;
import com.t4a.bridge.JavaMethodAction;
import lombok.extern.java.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses open weather to get prediction
 */
@Log
public class WeatherSearchExample {
    private String projectId = null;//"cookgptserver";
    private String location = null;//"us-central1";
    private String modelName = null;//"gemini-1.0-pro";

    private String promptText = null;//"Hey I am in Toronto do you think i can go out without jacket";
    public WeatherSearchExample(String[] args) throws Exception {
        if(args.length < 1) {
            throw new Exception("provide args in this format projectId=<> location=<> modelName=<> promptText=<>");
        }
        Map<String, String> argumentsMap = new HashMap<>();
        for (String arg : args) {
            // Split the argument into key and value using '=' as delimiter
            String[] parts = arg.split("=");

            // Ensure that the argument is correctly formatted with key and value
            if (parts.length == 2) {
                // Extract key and value
                String key = parts[0];
                String value = parts[1];

                // Store key-value pair in the map
                argumentsMap.put(key, value);
            } else {
                // Handle invalid arguments
                log.info("Invalid argument: " + arg);
            }
        }

        // Access values using the keys
        this.projectId = argumentsMap.get("projectId");
        this.location = argumentsMap.get("location");
        this.modelName = argumentsMap.get("modelName");
        this.promptText = argumentsMap.get("promptText");

        // Print the extracted values
        log.info("projectId: " + projectId);
        log.info("location: " + location);
        log.info("modelName: " + modelName);
        log.info("promptText: " + promptText);
    }
    public static void main(String[] args) throws Exception {

        WeatherSearchExample sample = new WeatherSearchExample(args);
        sample.actionOnPrompt();

    }
    public void actionOnPrompt() throws IOException, InvocationTargetException, IllegalAccessException {
        try (VertexAI vertexAI = new VertexAI(projectId, location)) {
            JavaMethodAction methodAction = new JavaMethodAction();
            FunctionDeclaration weatherFunciton = methodAction.buildFunction("com.external.HttpGetAction", "getTemprature", "getTemprature", "get weather for city");
            log.info("Function declaration h1:");
            log.info("" + weatherFunciton);

            JavaMethodAction additionalQuestion = new JavaMethodAction();
            FunctionDeclaration additionalQuestionFun = additionalQuestion.buildFunction("com.external.BlankAction", "askAdditionalQuestion", "askAdditionalQuestion", "ask remaining question");
            log.info("Function declaration h1:");
            log.info("" + additionalQuestionFun);
            //add the function to the tool
            Tool tool = Tool.newBuilder()
                    .addFunctionDeclarations(weatherFunciton).addFunctionDeclarations(additionalQuestionFun)
                    .build();


            GenerativeModel model =
                    GenerativeModel.newBuilder()
                            .setModelName(modelName)
                            .setVertexAi(vertexAI)
                            .setTools(Arrays.asList(tool))
                            .build();
            ChatSession chat = model.startChat();

            log.info(String.format("Ask the question 1: %s", promptText));
            GenerateContentResponse response = chat.sendMessage(promptText);

            log.info("\nPrint response 1 : ");
            log.info("" + ResponseHandler.getContent(response));
            log.info(methodAction.getPropertyValuesJsonString(response));

            Object obj = methodAction.action(response, new HttpGetAction());
            log.info(""+obj);

            Content content =
                    ContentMaker.fromMultiModalData(
                            PartMaker.fromFunctionResponse(
                                    "getTemprature", Collections.singletonMap("temperature",obj)));


            response = chat.sendMessage(content);

            log.info("Print response content: ");
            log.info(""+ResponseHandler.getContent(response));
            log.info(ResponseHandler.getText(response));


        }

    }

}
