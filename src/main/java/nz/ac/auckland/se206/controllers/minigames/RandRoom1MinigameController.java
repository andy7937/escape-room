package nz.ac.auckland.se206.controllers.minigames;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

/**
 * RandRoom1MinigameController is responsible for the Simon's game-like sequence minigame. In this
 * game, the player clicks on buttons, and with each successive round, an extra button is added to
 * the sequence. The objective is to complete 5 rounds to win the game.
 */
public class RandRoom1MinigameController extends MinigameController {

  // The grid pane containing the buttons. This is used to get the index of the button that was
  @FXML private GridPane gridPane;

  // The buttons in the grid pane that the player must click on.
  @FXML private Button button1;
  @FXML private Button button2;
  @FXML private Button button3;
  @FXML private Button button4;
  @FXML private Button button5;
  @FXML private Button button6;
  @FXML private Button button7;
  @FXML private Button button8;
  @FXML private Button button9;
  @FXML private ImageView image1;
  @FXML private ImageView image2;
  @FXML private ImageView image3;
  @FXML private ImageView image4;
  @FXML private ImageView image5;
  @FXML private ImageView image6;
  @FXML private ImageView image7;
  @FXML private ImageView image8;
  @FXML private ImageView image9;

  // The buttons and images in the grid pane. These are used to get the index of the button that was
  // clicked.
  private Button[][] buttons;
  private ImageView[][] imageViews;
  private List<Integer> sequence;
  private int sequenceNum = 0;
  private int round = 0;
  private boolean isHighlighting;

  /**
   * Initialize the buttons and images for the game and start the first round of the game. During
   * the first round, it highlights the sequence of buttons the player must click on.
   */
  public void initialize() {
    fadeIn();

    isSolved = false;
    isHighlighting = false;

    // Initialize the buttons and images.
    buttons =
        new Button[][] {
          {button1, button2, button3},
          {button4, button5, button6},
          {button7, button8, button9}
        };
    imageViews =
        new ImageView[][] {
          {image1, image2, image3},
          {image4, image5, image6},
          {image7, image8, image9}
        };

    // Initialize the sequence of buttons the player must click on.
    sequence = new ArrayList<>();

    // Start the first round of the game.
    startNewRound();
  }

  /**
   * Starts a new round of the game by adding a new button to the sequence and highlighting it. The
   * new button cannot be the same as the one before it to increase difficulty and avoid duplicates.
   */
  private void startNewRound() {
    round++;
    Random random = new Random();
    int lastNumber;

    // If the sequence is empty, set the last number to -1 to avoid duplicates.
    if (sequence.isEmpty()) {
      lastNumber = -1;

      // Otherwise, set the last number to the last number in the sequence.
    } else {
      lastNumber = sequence.get(sequence.size() - 1);
    }
    int newNumber;

    // Generate a new number that is not the same as the last number. This ensures that the new
    // button is not the same as the previous one.
    do {
      newNumber = random.nextInt(9);
    } while (newNumber == lastNumber);

    // Add the new number to the sequence and highlight the sequence.
    sequence.add(newNumber);
    sequenceNum = 0;

    highlightSequence();
  }

  /** Highlights the sequence of buttons that the player must click on. */
  private void highlightSequence() {
    isHighlighting = true;
    PauseTransition pause = new PauseTransition(Duration.seconds(1));
    pause.setOnFinished(event -> highlightNextButton());
    pause.play();
  }

  /**
   * Highlights the next button in the sequence and briefly clicks and unclicks it after 0.5
   * seconds.
   */
  private void highlightNextButton() {
    if (sequenceNum < round) {

      // Highlight the next button in the sequence. The sequence number is used to keep track of
      // which button to highlight.
      int imageIndex = sequence.get(sequenceNum);
      ImageView image = imageViews[imageIndex / 3][imageIndex % 3];

      // Click and unclick the button after 0.5 seconds. This is to indicate to the player which
      // button to click on.
      image.setImage(new Image("images/minigames/yellow_button.png"));

      PauseTransition pauseBetween = new PauseTransition(Duration.seconds(0.5));
      pauseBetween.setOnFinished(
          event -> {

            // Set the button back to the unclicked image.
            image.setImage(new Image("images/minigames/unclicked_button.png"));
            sequenceNum++;
            highlightNextButton();
          });
      pauseBetween.play();
    } else {
      isHighlighting = false;
      sequenceNum = 0;
    }
  }

  /**
   * Handles the button click event. If the player clicks on the correct button in the sequence, the
   * game progresses. If the player clicks on the wrong button, the game restarts.
   *
   * @param event the mouse event
   * @throws IOException if the game cannot be ended
   */
  @FXML
  private void onButtonClick(ActionEvent event) {
    if (isHighlighting) {
      return;
    }

    if (sequenceNum < sequence.size()) {
      Button clickedButton = (Button) event.getSource();
      int clickedIndex = getIndexFromButton(clickedButton);
      int expectedIndex = sequence.get(sequenceNum);
      ImageView image = imageViews[clickedIndex / 3][clickedIndex % 3];
      image.setImage(new Image("images/minigames/clicked_button.png"));

      if (clickedIndex == expectedIndex) {
        sequenceNum++;
        if (sequenceNum == sequence.size()) {
          if (sequence.size() == round) {
            if (round == 5) {

              // When the player finishes 5 rounds, they win the game.
              setAllButtonsGreen();
              System.out.println("You win!");
            } else {
              // If the player has finished the round, proceed to the next round.
              unclickButton(image);
              startNewRound();
            }
          }
        } else {
          unclickButton(image);
        }

      } else {

        // Restart the game if the player clicks the wrong button.
        setAllButtonsRed();

        sequence.clear();
        sequenceNum = 0;
        round = 0;

        startNewRound();
      }
    }
  }

  /**
   * Sets the button to the unclicked image.
   *
   * @param image the image to set
   */
  private void unclickButton(ImageView image) {
    PauseTransition pauseBetween = new PauseTransition(Duration.seconds(0.1));
    pauseBetween.setOnFinished(
        event -> {
          image.setImage(new Image("images/minigames/unclicked_button.png"));
        });
    pauseBetween.play();
  }

  /**
   * Gets the index of the button in the grid pane.
   *
   * @param button the button to get the index of
   * @return the index of the button
   */
  private int getIndexFromButton(Button button) {

    // Go through all the buttons and return the index of the button.
    for (int i = 0; i < buttons.length; i++) {
      for (int j = 0; j < buttons[i].length; j++) {
        if (buttons[i][j] == button) {

          // The index of the button is the row number multiplied by 3 plus the column number.
          return i * 3 + j;
        }
      }
    }
    return -1;
  }

  /**
   * Sets all the buttons to red, indicating that the player has lost the game and must restart it.
   * This method will be called when the player clicks on the wrong button.
   */
  private void setAllButtonsRed() {

    // Go through all the buttons and set them to red.
    for (int i = 0; i < buttons.length; i++) {
      for (int j = 0; j < buttons[i].length; j++) {

        // Get the image of the button and set it to red.
        ImageView image = imageViews[i][j];
        image.setImage(new Image("images/minigames/red_button.png"));
      }
    }

    // Set the buttons back to the unclicked image after 0.4 seconds.
    PauseTransition pause = new PauseTransition(Duration.seconds(0.4));
    pause.setOnFinished(event -> setAllButtonsToDefaultColor());
    pause.play();
  }

  /**
   * Sets all the buttons to green, indicating that the player has won the game. This method will be
   * called after the player has completed 5 rounds.
   */
  private void setAllButtonsGreen() {

    // Go through all the buttons and set them to green.
    for (int i = 0; i < buttons.length; i++) {
      for (int j = 0; j < buttons[i].length; j++) {
        ImageView image = imageViews[i][j];
        image.setImage(new Image("images/minigames/green_button.png"));
      }
    }

    // Set the buttons back to the unclicked image after 0.5 seconds.
    PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
    pause.setOnFinished(
        event -> {

          // Set the buttons back to the unclicked image and end the game.
          setAllButtonsToDefaultColor();
          endGame();
        });
    pause.play();
  }

  /**
   * Sets all the buttons to the default color. This method will be called after the buttons have
   * been set to red or green.
   */
  private void setAllButtonsToDefaultColor() {

    // Go through all the buttons and set them to the unclicked image.
    for (int i = 0; i < buttons.length; i++) {
      for (int j = 0; j < buttons[i].length; j++) {
        ImageView image = imageViews[i][j];
        image.setImage(new Image("images/minigames/unclicked_button.png"));
      }
    }
  }
}
