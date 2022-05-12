package mainGUI;

import Agent.Agent;
import AuctionHouse.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import Agent.AgentToAuction;
import javafx.scene.canvas.*;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Main extends Application {
    public final int WIDTH = 1000;
    public final int HEIGHT = 600;
    public Agent agent;
    public BorderPane border = new BorderPane();
    public AuctionHouse currHouse;
    public AgentToAuction currProxy;

    public String agentName = "Agent";
    public HashMap<AuctionHouse, String> auctionHouseNameMap = new HashMap<>();
    public HashMap<AgentToAuction, String> auctionMap = new HashMap<>();
    public List<String> allHouseNames = new ArrayList<>();
    public List<AgentToAuction> activeProxies = new ArrayList<>();
    private int lastIndex;
    private Map<Integer, Integer> localItemBidMap = new HashMap<>();

    @Override
    public void start(Stage primaryStage) throws IOException, InterruptedException {
        // Make a new agent on start
        agentName = getParameters().getUnnamed().get(2);
        String bankAddress = getParameters().getUnnamed().get(0);
        int bankPort = Integer.parseInt(getParameters().getUnnamed().get(1));
        agent = new Agent(100, agentName, bankAddress, bankPort);

        pullHouseNames();

        border.setLeft(showConnectButton());
        border.setCenter(items(0));
        border.setRight(showBankInfo());

        Scene scene = new Scene(border, WIDTH, HEIGHT);
        primaryStage.setTitle("Auction House");
        primaryStage.setScene(scene);
        primaryStage.show();

        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), this::repeatingFunctions));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * This function is called every .1 seconds. GUI elements that need to be refreshed go here
     * @param actionEvent
     */
    private void repeatingFunctions(ActionEvent actionEvent){
        border.setRight(showBankInfo());

        // Once we have connected to the auction houses, redraw their items constantly
        if(activeProxies != null) {

//            try {
//                border.setCenter(items(lastIndex));
//            } catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            }
        }

        if(agent.redrawTabsFlag) {
            try {
                border.setCenter(items(lastIndex));
                agent.redrawTabsFlag = false;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Show to initial button that tells the agent to connect to the auction houses
     * @return
     */
    public VBox showConnectButton(){
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(40,40,10,40));
        vbox.setSpacing(30);

        Text auctionHouseTitleText = new Text("Auction Houses");
        auctionHouseTitleText.setStyle("-fx-font: 18 arial;");
        vbox.getChildren().add(auctionHouseTitleText);

        Button connect = new Button("Connect to Auction Houses");
        connect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Connecting to Auction Houses");
                try {
                    connectToAuctionHouses();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                border.setLeft(showActionHouses());
            }
        });

        vbox.getChildren().add(connect);
        return vbox;
    }

    /**
     * Connects the agents to the auction houses
     */
    public void connectToAuctionHouses() throws IOException {
        List<String> availableHouses = agent.getAuctionConnects();

        for(String house : availableHouses){
            AgentToAuction proxy = agent.connectToAuctionHouse(house);
            String houseName = getHouseName(proxy);
            activeProxies.add(proxy);
        }
    }

    /**
     * Pull all the house names from the text file. Similar to the nouns/adjectives
     */
    public void pullHouseNames(){
        InputStream s1 = ClassLoader.getSystemResourceAsStream("houseNames.txt");
        Scanner sc = new Scanner(s1);
        while(sc.hasNext()) allHouseNames.add(sc.nextLine());
    }

    /**
     * Create the vbox that will hold the available auction houses
     * @return
     */
    public VBox showActionHouses(){
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(40,40,10,40));
        vbox.setSpacing(30);

        Text auctionHouseTitleText = new Text("Auction Houses");
        auctionHouseTitleText.setStyle("-fx-font: 18 arial;");
        vbox.getChildren().add(auctionHouseTitleText);

        if(activeProxies == null) return vbox;

        List<Button> auctionLinks = new ArrayList<>();
        for(int i = 0; i < activeProxies.size(); i++){
            Button newButton = auctionListButton(activeProxies.get(i));
            auctionLinks.add(newButton);
        }

        // Loop through the auction house buttons we made and add them to the vbox
        for (Button auctionLink : auctionLinks) {
            vbox.getChildren().add(auctionLink);
        }

        return vbox;
    }

    /**
     * Makes the button that changes which auction house the GUI is showing
     * @return the button
     */
    public Button auctionListButton(AgentToAuction proxy){
        // String houseName = auctionMap.get(proxy);
        String houseName = proxy.getHouseName();
        Button auctionButton = new Button(houseName);
        auctionButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                currProxy = proxy;
                try {
                    border.setCenter(items(0));
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        return auctionButton;
    }

    /**
     * Generate a house name for each of the proxies
     * @param proxy the proxy that needs a name
     * @return the assigned name
     */
    public String getHouseName(AgentToAuction proxy){
        int r1 = (int)(Math.random() * allHouseNames.size());
        if(auctionMap.containsValue(allHouseNames.get(r1))) return getHouseName(proxy);
        else auctionMap.put(proxy, allHouseNames.get(r1));

        return allHouseNames.get(r1);
    }

    /**
     * Creates the tab panes that holds all the values from the current auciton house
     * @param index the tab to display first
     * @return
     */
    public TabPane items(int index) throws IOException, InterruptedException {
        TabPane itemPane = new TabPane();
        itemPane.setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderWidths.DEFAULT)));

        if(currProxy == null) return itemPane; // Don't try to build the tabs if there are no auction houses
        List<Item> houseItems = agent.getItems(currProxy);

        int i = 0; // Keep track of the tab's index
        for(Item item : houseItems){
            Tab itemTab = itemTab(item, i++);
            itemPane.getTabs().add(itemTab);
        }

        SingleSelectionModel<Tab> selectionModel = itemPane.getSelectionModel();
        selectionModel.select(index);
        lastIndex = selectionModel.getSelectedIndex();

        return itemPane;
    }

    /**
     * Creates one tab that presents the individual listing
     * @param item the individual object
     * @return the created tab
     */
    public Tab itemTab(Item item, int index){

        String itemName = item.description;
        Tab tab = new Tab(itemName);
        VBox mainVBox = new VBox();
        mainVBox.setSpacing(40);

        // Set the title of the item for sale
        VBox itemTitle = new VBox();
        itemTitle.setPadding(new Insets(30,10,0,10));
        Text itemDescription = new Text(itemName);
        itemDescription.setStyle("-fx-font: 16 arial;");
        itemTitle.getChildren().add(itemDescription);

        // Add the item ID
        VBox itemIDBox = new VBox();
        Text itemID = new Text("Item number " + item.auctionID);
        itemIDBox.setPadding(new Insets(0,10,0,10));
        itemID.setStyle("-fx-font: 14 arial;");
        itemIDBox.getChildren().add(itemID);

        localItemBidMap.put(item.auctionID, item.currentBid);

        int currBid = currProxy.getItemBid(item.auctionID);
        int minimumBid = currBid + 1;

        // Add the current bid price,
        VBox curBidVBox = new VBox();
        curBidVBox.setPadding(new Insets(0,10,0,10));
        Text curBidText = new Text("Leading bid: $" + currBid); //@todo get the current bid from the auction
        curBidText.setStyle("-fx-font: 14 arial;");
        curBidVBox.getChildren().add(curBidText);

        // Add the HBox which contains the bid text field
        HBox makeBidHBox = new HBox();
        Text makeBidText = new Text("Make a bid of $" + minimumBid + " or more     ");
        TextField typeBid = new TextField();
        Button submitBid = new Button("Submit Bid");
        submitBid.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //@todo call make bid function after checking if agent can bid given amount
                if(typeBid.getText().equals("")) return; // Don't allow empty bids
                int bidAmount = Integer.parseInt(typeBid.getText());
                if(bidAmount <= agent.avaliableBalance){
                    System.out.println("Bid submitted at a value of: " + bidAmount);
                    try {
                        agent.makeBid(currProxy, bidAmount, item.auctionID);
                        border.setCenter(items(index));
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }


                }else{
                    agent.statusMessages.add("You don't have that much!");
                }

            }
        });

        makeBidHBox.getChildren().addAll(makeBidText, typeBid, submitBid);

        mainVBox.getChildren().addAll(itemTitle, itemIDBox, curBidVBox, makeBidHBox);
        tab.setContent(mainVBox);
        return tab;
    }

    /**
     * Add the vbox that will hold the agent's bank information
     * @return vbox
     */
    public VBox showBankInfo(){
        VBox vbox = new VBox();
        vbox.setSpacing(30);
        vbox.setPadding(new Insets(30,50,30,30));
        vbox.setStyle("-fx-background-color: #e4ebf7;");

        // Display agent name
        Text agentNameText = new Text(agentName + "'s Bank Account");
        agentNameText.setStyle("-fx-font: 24 arial;");

        // Display agent account number
        Text agentAccountText = new Text("Account Number: " + agent.getAccountNumber());
        agentAccountText.setStyle("-fx-font: 16 arial;");

        // Display Total Agent Balance
        Text totalBalanceText = new Text("Total Balance: $" + agent.balance);
        totalBalanceText.setStyle("-fx-font: 16 arial;");

        // Display the amount the agent has in pending purchases
        HBox pendingBidsHbox = new HBox();

        Text pendingBidsText = new Text("Pending Bids:  ");
        pendingBidsText.setStyle("-fx-font: 16 arial;");
        Text pendBdsAmmText = new Text("-$"+ (agent.getPendingBids()));
        pendBdsAmmText.setFill(Color.RED);
        pendBdsAmmText.setStyle("-fx-font: 16 arial;");
        pendingBidsHbox.getChildren().addAll(pendingBidsText, pendBdsAmmText);

        // Display the amount the agent has to spend (total - pending bids)
        Text availableFundsText = new Text("Available: $" + agent.avaliableBalance);
        availableFundsText.setStyle("-fx-font: 16 arial;");

        VBox statMsgsVbox = new VBox();
        Text statMsgHeader = new Text("Messages: ");
        statMsgsVbox.getChildren().add(statMsgHeader);
        if(agent.statusMessages.size() > 5){ // Only print out the 5 newest messages
            for(int i = agent.statusMessages.size()-5; i < agent.statusMessages.size(); i++){
                String statMsg = agent.statusMessages.get(i);
                Text statusMessage = new Text(" -" + statMsg);
                statMsgsVbox.getChildren().add(statusMessage);
            }
        }else {
            for (String statMsg : agent.statusMessages) {
                Text statusMessage = new Text(" -" + statMsg);
                statMsgsVbox.getChildren().add(statusMessage);
            }
        }

        vbox.getChildren().addAll(agentNameText, agentAccountText, totalBalanceText, pendingBidsHbox, availableFundsText, statMsgsVbox);
        return vbox;
    }

    /**
     * arg[0] Bank Address
     * arg[1] Bank Port
     * arg[2] Agent Name
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }

}
