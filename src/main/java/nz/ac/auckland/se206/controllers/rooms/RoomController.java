package nz.ac.auckland.se206.controllers.rooms;

import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import nz.ac.auckland.se206.GameMediaPlayer;
import nz.ac.auckland.se206.GameState;
import nz.ac.auckland.se206.gpt.ChatMessage;

/**
 * Abstract class for room controllers. Contains methods for handling the click and hover events on
 * objects, as well as the click events on the arrows. Also has the popup for when a part is found.
 */
public abstract class RoomController {
  @FXML protected Label actionLabel;
  @FXML protected Rectangle grayRectangle;
  @FXML protected Rectangle okayRectangle;
  @FXML protected Rectangle fadeRectangle;
  @FXML protected ImageView arrow1;
  @FXML protected ImageView arrow2;
  @FXML protected ImageView background;
  @FXML protected ImageView partFoundTitleImage;
  @FXML protected ImageView partFoundContentImage;
  @FXML protected ImageView partFoundOkayImage;
  @FXML protected ImageView alienImage;

  @FXML protected Button muteButton;
  @FXML protected Button flavourTextButton;
  @FXML protected ImageView mute;
  @FXML protected ImageView flavourTextImage;

  protected Thread backgroundThread;
  protected Thread flagThread;
  protected Thread alienThread;
  protected boolean visited;
  protected FadeTransition ft;

  @FXML
  private void initialize() {
    fadeIn();

    // Check if minigame popup shown after solved
    if (!GameState.getPartFoundPopupShown() && GameState.getMinigameSolved()) {
      GameState.setPartFoundPopupShown();
      showPopup();
    }
    animate();
    alienThread = new Thread(() -> animateAlien());
    alienThread.setDaemon(true);
    alienThread.start();
  }

  protected void stopThreads() {
    if (backgroundThread != null) {
      backgroundThread.interrupt();
    }
    if (flagThread != null) {
      flagThread.interrupt();
    }
    if (alienThread != null) {
      alienThread.interrupt();
    }
  }

  /** Show the part found popup. */
  protected void showPopup() {
    // Set the correct image for the contents of the popup
    switch (GameState.partsFound) {
      case 1:
        partFoundContentImage.setImage(new Image("/images/rooms/partsleft2.png"));
        break;
      case 2:
        partFoundContentImage.setImage(new Image("/images/rooms/partsleft1.png"));
        break;
      case 3:
        partFoundContentImage.setImage(new Image("/images/rooms/partsleft0.png"));
        break;
      default:
        partFoundContentImage.setImage(new Image("/images/rooms/partsleft0.png"));
        break;
    }

    // Show the popup
    grayRectangle.setVisible(true);
    okayRectangle.setVisible(true);
    partFoundTitleImage.setVisible(true);
    partFoundContentImage.setVisible(true);
    partFoundOkayImage.setVisible(true);
  }

  /**
   * Displays a dialog from the AI system with the given content in the chat box.
   *
   * @param dialog the content of the dialog
   */
  protected void showDialog(String dialog) {
    ChatMessage chat = new ChatMessage("assistant", dialog);
    GameState.showChatMessage(chat);
    GameMediaPlayer.playNotificationSound();
  }

  /**
   * Handles the click event on the okay button in the part found popup.
   *
   * @param event the mouse event
   */
  @FXML
  protected void clickOkay(MouseEvent event) {
    grayRectangle.setVisible(false);
    okayRectangle.setVisible(false);
    partFoundTitleImage.setVisible(false);
    partFoundContentImage.setVisible(false);
    partFoundOkayImage.setVisible(false);
  }

  /**
   * Handles the click event on an object.
   *
   * @param event the mouse event
   * @throws IOException if the FXML file for the next room cannot be found
   */
  @FXML
  protected void clickObject(MouseEvent event) {
    Shape object = (Shape) event.getSource();
    String objectId = object.getId();

    if (GameState.isRiddleAnswerCorrect(objectId)) {
      // Correct object clicked. Increment parts found and set flag.
      GameState.incrementPartsFound();
      GameState.isObjectFound = true;
      showPopup();
    } else {
      // Not the correct object. Provide some flavour text.
      if (!GameState.isFlavourTextEnabled) {
        return;
      }

      GameState.setAlienHead();
      GameState.sayFlavourText(objectId);
    }
  }

  /**
   * Handles the hover event on an object and updates the action label based on the object's ID.
   *
   * @param event the mouse event triggered by hovering over the object
   */
  @FXML
  protected void hoverObject(MouseEvent event) {
    // Get the object that triggered the event
    Rectangle object = (Rectangle) event.getSource();

    // Retrieve the object's unique identifier (ID)
    String objectId = object.getId();

    // Obtain the scene containing the object
    Scene scene = object.getScene();

    // Locate the corresponding image associated with the object
    ImageView image = (ImageView) scene.lookup("#" + objectId);

    // Change the displayed image to indicate the object is selected
    image.setImage(new Image("/images/objects/" + objectId + "_selected.png"));

    // Update the action label based on the object's ID
    if (objectId.equals("arrow1")) {
      actionLabel.setText("Explore area to the left");
      return;
    } else if (objectId.equals("arrow2")) {
      actionLabel.setText("Explore area to the right");
      return;
    }

    // Default action label when hovering over other objects
    actionLabel.setText("Search object");
  }

  /**
   * Handles the event when the mouse pointer stops hovering over an object and resets its
   * appearance.
   *
   * @param event the mouse event triggered when unhovering over the object
   */
  @FXML
  protected void unhoverObject(MouseEvent event) {
    // Get the object that triggered the event
    Rectangle object = (Rectangle) event.getSource();

    // Retrieve the object's unique identifier (ID)
    String objectId = object.getId();

    // Obtain the scene containing the object
    Scene scene = object.getScene();

    // Locate the corresponding image associated with the object
    ImageView image = (ImageView) scene.lookup("#" + objectId);

    // Restore the default image for the object
    image.setImage(new Image("/images/objects/" + objectId + ".png"));

    // Clear the action label when unhovering over the object
    actionLabel.setText("");
  }

  /**
   * Handles the click event on the minigame object.
   *
   * @param event the mouse event
   */
  @FXML
  protected void clickMinigame(MouseEvent event) {

    // Check if minigame has been solved
    if (GameState.getMinigameSolved()) {
      showPopup();
      return;
    }
    stopThreads();
    GameState.inMinigame = true;
    String fxmlPath = GameState.currRooms.get(GameState.getCurrRoom()) + "minigame";
    fadeOut();
    ft.setOnFinished(
        event2 -> {
          fadeRectangle.setVisible(false);
          GameState.switchRoom(fxmlPath);
        });
    ft.play();
  }

  /**
   * Handles the click event on the left arrow.
   *
   * @param event the mouse event
   */
  @FXML
  private void clickArrow1(MouseEvent event) {
    // Stops all threads when switching rooms
    stopThreads();

    // Initiates the fade transition
    fadeOut();
    ft.setOnFinished(
        event2 -> {
          fadeRectangle.setVisible(false);
          GameState.prevRoom();
        });
    ft.play();
  }

  /**
   * Handles the click event on the right arrow.
   *
   * @param event the mouse event
   */
  @FXML
  private void clickArrow2(MouseEvent event) {
    // Stops all threads when switching rooms
    stopThreads();

    // Initiates the fade transition
    fadeOut();
    ft.setOnFinished(
        event2 -> {
          fadeRectangle.setVisible(false);
          GameState.nextRoom();
        });
    ft.play();
  }

  @FXML
  protected void hoverAlien(MouseEvent event) {
    actionLabel.setText("Talk to alien");
    changeAlienImage(alienImage.getId(), "_selected");
  }

  @FXML
  protected void unhoverAlien(MouseEvent event) {
    actionLabel.setText("");
    changeAlienImage(alienImage.getId(), "");
  }

  /**
   * Continuously animates the alien character's talking expression based on the state of the game.
   * The animation consists of cycling through different talking frames at regular intervals.
   */
  protected void animateAlien() {
    while (true) {
      // Check if the alien is currently talking
      while (GameState.getAlienTalking()) {
        try {
          // Display talking frame 1
          changeAlienImage(alienImage.getId(), "_talking1");
          Thread.sleep(200);

          // Display talking frame 2
          changeAlienImage(alienImage.getId(), "_talking2");
          Thread.sleep(200);

          // Display talking frame 3
          changeAlienImage(alienImage.getId(), "_talking3");
          Thread.sleep(200);

          // Revert to talking frame 2
          changeAlienImage(alienImage.getId(), "_talking2");
          Thread.sleep(200);

          // Check if the alien has stopped talking during animation
          if (!GameState.getAlienTalking()) {
            // Reset the alien's image to the default state
            changeAlienImage(alienImage.getId(), "");
          }
        } catch (InterruptedException e) {
          // Handle interruption gracefully
          Thread.currentThread().interrupt();
          return;
        }
      }

      // Delay for a second before checking the talking state again
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // Handle interruption gracefully
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  /**
   * Changes the image of the alien.
   *
   * @param objectId the ID of the alien
   * @param stageOfAnimation the stage of the animation
   */
  public void changeAlienImage(String objectId, String stageOfAnimation) {
    alienImage.setImage(new Image("/images/" + objectId + stageOfAnimation + ".png"));
  }

  /**
   * Animates the background.
   *
   * @throws InterruptedException if the thread is interrupted
   */
  protected void animate() {
    Task<Void> animation =
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            while (true) {
              Thread.sleep(500);
              Platform.runLater(
                  () -> {
                    // Animate 1
                    background.setImage(new Image("/images/rooms/background2.png"));
                  });
              Thread.sleep(500);
              Platform.runLater(
                  () -> {
                    // Animate 2
                    background.setImage(new Image("/images/rooms/background1.png"));
                  });
            }
          }
        };
    backgroundThread = new Thread(animation);

    backgroundThread.setDaemon(true);
    backgroundThread.start();
  }

  /** Sets the text of the mute button according to GameState.isMuted. */
  protected void setMuteButtonText() {
    String name = (GameState.isMuted) ? "unmute" : "mute";
    setButtonImage(name, mute);
  }

  /** Sets the text of the flavour text button according to GameState.isFlavourTextEnabled. */
  protected void setFlavourTextButtonText() {
    String name = (GameState.isFlavourTextEnabled) ? "disable_flavour_text" : "enable_flavour_text";
    setButtonImage(name, flavourTextImage);
  }

  @FXML
  protected void hoverFlavourTextButton() {
    String name =
        (GameState.isFlavourTextEnabled)
            ? "disable_flavour_text_selected"
            : "enable_flavour_text_selected";
    setButtonImage(name, flavourTextImage);
  }

  @FXML
  protected void unhoverFlavourTextButton() {
    String name = (GameState.isFlavourTextEnabled) ? "disable_flavour_text" : "enable_flavour_text";
    setButtonImage(name, flavourTextImage);
  }

  /** Changes the mute button to selected image when hovered over. */
  @FXML
  protected void hoverMuteButton() {
    String name = (GameState.isMuted) ? "unmute_selected" : "mute_selected";
    setButtonImage(name, mute);
  }

  /** Changes the mute button to unselected image when not hovered over. */
  @FXML
  protected void unhoverMuteButton() {
    String name = (GameState.isMuted) ? "unmute" : "mute";
    setButtonImage(name, mute);
  }

  protected void setButtonImage(String name, ImageView image) {
    image.setImage(new Image("/images/objects/" + name + ".png"));
  }

  /**
   * Mutes/unmutes the game audio.
   *
   * @param event the event that triggered this method
   */
  @FXML
  protected void onClickMute(ActionEvent event) {
    GameState.toggleMute();
    setMuteButtonText();
  }

  /**
   * Enables or disables object flavour text.
   *
   * @param event the event that triggered this method
   */
  @FXML
  protected void onClickFlavourText(ActionEvent event) {
    GameState.toggleFlavourText();
    setFlavourTextButtonText();
  }

  /** Fades in the room when the player enters. */
  protected void fadeIn() {
    fadeRectangle.setOpacity(1.0);
    fadeRectangle.setVisible(true);
    ft = new FadeTransition(Duration.millis(300), fadeRectangle);
    ft.setFromValue(1.0);
    ft.setToValue(0.0);

    // makes sure the fade rectangle is hidden after the fade in
    ft.setOnFinished(
        event -> {
          fadeRectangle.setVisible(false);
        });

    ft.play();
  }

  /**
   * Sets up the fade out for the room. Need to manually play ft after setting destination room in
   * setOnFinished
   */
  protected void fadeOut() {
    ft = new FadeTransition(Duration.millis(300), fadeRectangle);
    ft.setFromValue(0.0);
    ft.setToValue(1.0);
    fadeRectangle.setVisible(true);
  }
}
