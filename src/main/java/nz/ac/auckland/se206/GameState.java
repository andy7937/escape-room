package nz.ac.auckland.se206;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import javafx.scene.image.Image;
import nz.ac.auckland.se206.controllers.ChatController;
import nz.ac.auckland.se206.controllers.GameController;
import nz.ac.auckland.se206.gpt.ChatMessage;
import nz.ac.auckland.se206.gpt.GptPromptEngineering;
import nz.ac.auckland.se206.gpt.openai.ChatCompletionRequest;

/** Represents the state of the game. */
public class GameState {

  /** Indicates whether the riddle has been resolved. */
  public static boolean isRiddleResolved = false;

  /** Indicates whether the object in the room has been found. */
  public static boolean isObjectFound = false;

  /** Indicates whether the game was won. */
  public static boolean gameWon = true;

  /** Reference to chat controller. */
  private static ChatController chatController;

  /** Reference to game controller. */
  private static GameController gameController;

  /** The hashmap to array of riddle answers. */
  private static HashMap<String, ArrayList<String>> riddleAnswers =
      new HashMap<String, ArrayList<String>>();

  /** The current answer to the riddle. */
  private static String currRiddleAnswer;

  /** The current riddle. */
  public static String currRiddle;

  /** The randomised rooms from text file. */
  public static ArrayList<String> randomRooms = new ArrayList<String>();

  /** Rooms in current game. */
  public static ArrayList<String> currRooms = new ArrayList<String>();

  /** Whether or not minigame was solved in each room. */
  public static ArrayList<Boolean> minigameSolved = new ArrayList<Boolean>();

  /** Whether or not part found popup has been shown for minigame in room. */
  public static ArrayList<Boolean> partFoundPopupShown = new ArrayList<Boolean>();

  /** Whether or not the alien in the room is currently talking. */
  public static ArrayList<Boolean> alienTalking = new ArrayList<Boolean>();

  /** The current loaded room as index in currRooms array. */
  public static int currRoom;

  /** The chat completion requests for each room. */
  public static ArrayList<ChatCompletionRequest> roomChatCompletionRequests =
      new ArrayList<ChatCompletionRequest>();

  /** The number of hints given. */
  public static int hintsCounter;

  /** The number of hints allowed. */
  public static int hintsAllowed;

  /** The time limit for the game. */
  public static int timeLimit;

  /** The number of parts found. */
  public static int partsFound;

  /** Whether or not user is in a minigame. */
  public static boolean inMinigame;

  /** Whether audio has been muted or not. */
  public static boolean isMuted;

  /** Whether or not to allow flavour text. */
  public static boolean isFlavourTextEnabled = true;

  /** The hashmap of alien heads. */
  public static final HashMap<String, String> alienHeads =
      new HashMap<String, String>() {
        {
          put("mainroom", "alien.png");
          put("randroom1", "alien1.png");
          put("randroom2", "alien3.png");
          put("randroom3", "alien2.png");
          put("randroom4", "alien4.png");
        }
      };

  static {
    // Read random rooms and their riddle answers from file
    try {
      InputStream isRoom = App.class.getResourceAsStream("/randomRooms.txt");
      BufferedReader readerRoom = new BufferedReader(new InputStreamReader(isRoom));

      while (true) {
        String line = readerRoom.readLine();
        if (line == null) {
          break;
        }
        randomRooms.add(line);

        ArrayList<String> temp = new ArrayList<String>();

        // Read riddle answers from the room
        try {
          InputStream isRiddle = App.class.getResourceAsStream("/answers/" + line + ".txt");
          BufferedReader readerRiddle = new BufferedReader(new InputStreamReader(isRiddle));

          while (true) {
            String line2 = readerRiddle.readLine();
            if (line2 == null) {
              break;
            }

            temp.add(line2);
          }
        } catch (Exception e) {
          System.out.println("Could not read from riddleAnswers.txt");
        }

        riddleAnswers.put(line, temp);
      }
    } catch (Exception e) {
      System.out.println("Could not read from randomRooms.txt");
    }
  }

  /**
   * Sets the game controller for the game.
   *
   * @param controller the game controller
   */
  public static void setGameController(GameController controller) {
    gameController = controller;
  }

  /**
   * Switches the current room to the given room.
   *
   * @param room the room to switch to
   */
  public static void switchRoom(String room) {
    gameController.switchRoom(room);
  }

  /**
   * Sets the chat controller for the game.
   *
   * @param controller the chat controller
   */
  public static void setChatController(ChatController controller) {
    chatController = controller;
  }

  /**
   * Asks the GPT model the given request.
   *
   * @param request the request to ask
   */
  public static void askGpt(String request) {
    chatController.askGpt(request);
  }

  /** Returns a hint prompt depending on game state. */
  public static String getHint() {
    String roomName = currRooms.get(currRoom);
    String hint;

    // User has all parts
    if (partsFound == 3) {
      if (inMinigame) {
        // User in rocket minigame
        return "Tell the user to use the slider on the right to change amplitude and the slider at"
            + " the top to change frequency of the sine wave so that the 2 waves match."
            + " Explain how amplitude affects height and frequency affects how close the"
            + " waves are within 40 words";
      } else {
        // User in one of the rooms
        return "Ask user to go to the rocket";
      }
    }

    // Think about improving the hint system by actually giving the state of the minigame
    if (inMinigame) {
      // User is doing a minigame
      switch (roomName) {
        case "randroom1":
          return "Tell the user to click the flashing yellow lights in the shown order to unlock"
              + " the UFO's storage. Recommend writing down the order if it is too hard to"
              + " remember. Respond in 30 words";
        case "randroom3":
          return "Tell the user to click on each tooth of the plant's top row of teeth so that they"
              + " match the bottom row. Give a recommendation on how to solve this if this"
              + " is too hard to do. Respond in 30 words";
        case "randroom2":
          return "Tell the user to click quickly in order to increase the pressure of the oxygen"
              + " canister, and to keep the pressure between the red lines. Tell them not to"
              + " let the pressure be too low or high. Respond in 30 words";
        case "randroom4":
          return "Tell the user to match up all the pairs of symbols by clicking on a tile to"
              + " reveal a symbol. Then, give one of the following recommendations: only"
              + " match 1 symbol at a time or reveal all the tiles first, or write things"
              + " down if the user finds it too hard. Respond in 30 words";
        default:
          return "Tell the user to talk to aliens";
      }
    }

    // Tell user to talk to alien by rocket to get a riddle
    if (currRiddle == null) {
      if (roomName.equals("mainroom")) {
        return "Ask the user to come to you for a riddle. Do not say a riddle";
      } else {
        return "Ask the user to talk to the alien by the rocket for a riddle. Do not say a"
            + " riddle";
      }
    }

    // User has found 2 parts
    if (partsFound == 2) {
      // User hasnt solved the riddle yet
      if (!isRiddleResolved) {
        if (!roomName.equals("mainroom")) {
          return "Tell the user to solve the riddle of the alien by the rocket. Do not give a"
              + " riddle. Respond in 20 words";
        }
      }

      if (isRiddleResolved && !isObjectFound) {
        // User has resolved riddle, but not yet found the object
        return "Tell the user to look for the object that is the answer to the riddle. Respond in"
            + " 20 words";
      }
    }

    // User in one of the rooms, and not a minigame, and the riddle has already been given
    switch (roomName) {
      case "mainroom":
        return "Give a short hint for a riddle with the answer "
            + currRiddleAnswer
            + ". Do not say the word "
            + currRiddleAnswer;
      case "randroom1":
        hint = "Tell the user the part is in the crashed UFO. Respond in 20 words";
        break;
      case "randroom2":
        hint =
            "Tell the user you will give the part after stabilising the oxygen canister. Respond in"
                + " 20 words";
        break;
      case "randroom3":
        hint = "Tell the user the part is in the alien plant. Respond in 20 words";
        break;
      case "randroom4":
        hint = "Tell the user the part is in the dark cave. Respond in 20 words";
        break;
      default:
        hint = "Tell the user to talk to aliens";
    }
    if (GameState.getMinigameSolved()) {
      // Minigame in room has been solved, so ask user to go to another room
      return "Tell the user to try approaching fellow aliens in each room for an idea of"
          + " where the parts are. Respond in 20 words";
    } else {
      return hint;
    }
  }

  /** Toggles the mute flag for audio. */
  public static void toggleMute() {
    isMuted = !isMuted;
  }

  /** Toggles the flavour text flag. */
  public static void toggleFlavourText() {
    isFlavourTextEnabled = !isFlavourTextEnabled;
  }

  /** Sets the alien head image in the chat controller. */
  public static void setAlienHead() {
    Image alienImage = new Image("/images/chatroom/" + alienHeads.get(currRooms.get(currRoom)));
    chatController.setAlienHeadImage(alienImage);
  }

  /** Gets AI to say some flavour text. */
  public static void sayFlavourText(String object) {
    chatController.sayFlavourText(object);
  }

  /** Puts message into chatbox if it is not already translating. */
  public static void showChatMessage(ChatMessage chat) {
    if (chatController.getIsTranslating()) {
      return;
    }

    chatController.appendChatMessage(chat.getContent());
  }

  /** Sets the current riddle answer to a random answer from the list of riddle answers. */
  public static void setRandomCurrRiddleAnswer() {
    ArrayList<String> temp = new ArrayList<String>();
    temp.addAll(riddleAnswers.get(currRooms.get(1)));
    temp.addAll(riddleAnswers.get(currRooms.get(2)));

    int index = (int) (Math.random() * temp.size());
    currRiddleAnswer = temp.get(index);
    System.out.println(currRiddleAnswer);
  }

  /**
   * Checks if the object being pointed at is correct and if the riddle has been resolved.
   *
   * @param object the object to check
   * @return true if the object is correct, false otherwise
   */
  public static Boolean isRiddleAnswerCorrect(String object) {
    return isRiddleResolved && !isObjectFound && object.equals(currRiddleAnswer);
  }

  /**
   * Returns the results of the game as a string.
   *
   * @return the results of the game as a string
   */
  public static String getResults() {
    if (gameWon) {
      return "Won!";
    } else {
      return "Lost!";
    }
  }

  /**
   * Returns the current riddle answer.
   *
   * @return the current riddle answer
   */
  public static String getCurrRiddleAnswer() {
    return currRiddleAnswer;
  }

  /** Chooses 2 random rooms from the pool of 4. */
  public static void setRandomRooms() {
    Random rand = new Random();
    ArrayList<String> randomRoomsCopy = (ArrayList<String>) randomRooms.clone();

    // Get random room from array, then remove from array
    int randInt = rand.nextInt(randomRooms.size());
    currRooms.add(randomRoomsCopy.get(randInt));
    randomRoomsCopy.remove(randInt);

    // Get a different random room from array
    randInt = rand.nextInt(randomRooms.size() - 1);
    currRooms.add(randomRoomsCopy.get(randInt));

    // Set chat completion requests for each room
    for (int i = 0; i < 3; i++) {
      ChatCompletionRequest chatCompletionRequest =
          new ChatCompletionRequest().setN(1).setTemperature(0.7).setTopP(0.5).setMaxTokens(100);

      // Provide context to ai
      String context = GptPromptEngineering.getAlienContext(currRooms.get(i));

      ChatMessage msg = new ChatMessage("system", context);
      chatCompletionRequest.addMessage(msg);

      roomChatCompletionRequests.add(chatCompletionRequest);
    }
  }

  /** Returns the chat completion request for the current room. */
  public static ChatCompletionRequest getChatCompletionRequest() {
    return roomChatCompletionRequests.get(currRoom);
  }

  /** Returns the current room index. */
  public static int getCurrRoom() {
    return currRoom;
  }

  /** Switches to previous room from the current room in the game. */
  public static void prevRoom() {
    currRoom--;

    if (currRoom < 0) {
      currRoom += currRooms.size();
    }

    switchRoom(currRooms.get(currRoom));
  }

  /** Switches to next room from the current room. */
  public static void nextRoom() {
    currRoom++;

    if (currRoom == currRooms.size()) {
      currRoom -= currRooms.size();
    }

    switchRoom(currRooms.get(currRoom));
  }

  /** Increment the parts found, and update label in chat controller. */
  public static void incrementPartsFound() {
    partsFound++;
    chatController.setPartsCounter(partsFound);
  }

  /**
   * Returns whether or not the part found popup has been shown for the minigame in the current
   * room.
   *
   * @return true if the part found popup has been shown, false otherwise
   */
  public static boolean getPartFoundPopupShown() {
    return partFoundPopupShown.get(currRoom);
  }

  /** Sets the partFoundPopupSHown value for current room to true. */
  public static void setPartFoundPopupShown() {
    partFoundPopupShown.set(currRoom, true);
  }

  /** Sets the number of hints allowed for the game. */
  public static void setHintsAllowed(String difficulty) {
    if (difficulty.equals("easy")) {
      hintsAllowed = Integer.MAX_VALUE;
    } else if (difficulty.equals("medium")) {
      hintsAllowed = 5;
    } else {
      hintsAllowed = 0;
    }
  }

  /**
   * Toggles whether or not the alien in the current room is talking.
   *
   * @param room the room to toggle
   */
  public static void toggleAlienTalking(int room) {
    alienTalking.set(room, !alienTalking.get(room));
  }

  /** Returns whether or not the alien in the current room is talking. */
  public static boolean getAlienTalking() {
    return alienTalking.get(currRoom);
  }

  /**
   * Check whether minigame in current room has been solved.
   *
   * @return true if minigame has been solved, false otherwise
   */
  public static boolean getMinigameSolved() {
    return minigameSolved.get(currRoom);
  }

  /** Sets the minigame in the current room as solved. */
  public static void setMinigameSolved() {
    minigameSolved.set(currRoom, true);
  }

  /** Sets the time limit for the game. */
  public static void setTimeLimit(int minutes) {
    timeLimit = minutes * 60;
  }

  /**
   * Checks if a hint is available.
   *
   * @return true if a hint is available, false otherwise
   */
  public static boolean isHintAvailable() {
    return hintsCounter < hintsAllowed;
  }

  /** Resets all flags in the game, making it ready for the next round. */
  public static void resetGameState() {
    isRiddleResolved = false;
    isObjectFound = false;
    currRiddle = null;
    gameWon = true;
    inMinigame = false;
    hintsCounter = 0;
    partsFound = 0;
    currRoom = 0;

    // Reset minigameSolved and partFoundPopupShown
    minigameSolved.clear();
    partFoundPopupShown.clear();
    alienTalking.clear();
    for (int i = 0; i < 3; i++) {
      minigameSolved.add(false);
      partFoundPopupShown.add(false);
      alienTalking.add(false);
    }

    currRooms.clear();
    currRooms.add("mainroom");
    roomChatCompletionRequests.clear();
  }
}
