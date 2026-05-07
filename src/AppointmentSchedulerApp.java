import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.geometry.VPos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JavaFX application for managing clients and appointments.
 */
public class AppointmentSchedulerApp extends Application {

    private static final double WELCOME_SIZE = 26;
    private static final double SUBTITLE_SIZE = 14;
    private static final double BUTTON_MIN_WIDTH = 160;
    private static final DateTimeFormatter DOB_FORMAT = DateTimeFormatter.ofPattern("MM/dd/uuuu");
    private static final DateTimeFormatter APPT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/uuuu");

    private Stage primaryStage;
    private Label headerTitleLabel;
    private Label headerSubtitleLabel;
    private Label statusLabel;
    private ClientService clientService;
    private AppointmentService appointmentService;
    private TableView<Client> clientsTable;
    private TextField clientSearchField;
    private BorderPane root;
    private TableView<Appointment> apptsByRangeTable;
    private TableView<Appointment> apptsByClientTable;
    private UUID apptsByClientClientId;
    private UUID pendingSelectClientId;
    private LocalDate appointmentsAnchorDate = LocalDate.now();
    private Label appointmentsRangeLabel;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.clientService = new ClientService(new InMemoryClientRepo());
        this.appointmentService = new AppointmentService(new InMemoryAppointmentRepo());

        headerTitleLabel = new Label("Appointment Scheduler");
        headerTitleLabel.setFont(Font.font(null, FontWeight.BOLD, WELCOME_SIZE));
        headerTitleLabel.setMaxWidth(Double.MAX_VALUE);
        headerTitleLabel.setAlignment(Pos.CENTER);

        headerSubtitleLabel = new Label();
        headerSubtitleLabel.setFont(Font.font(null, FontWeight.NORMAL, SUBTITLE_SIZE));
        headerSubtitleLabel.setMaxWidth(Double.MAX_VALUE);
        headerSubtitleLabel.setAlignment(Pos.CENTER);
        headerSubtitleLabel.setWrapText(true);
        setHeaderSubtitleHome();

        VBox header = new VBox(6, headerTitleLabel, headerSubtitleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(24, 24, 16, 24));

        statusLabel = new Label("Choose an action to get started.");
        statusLabel.setFont(Font.font(null, FontWeight.NORMAL, 13));
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setPadding(new Insets(16, 24, 20, 24));
        statusLabel.setStyle("-fx-background-color: #ececec; -fx-border-color: #c8c8c8; -fx-border-width: 1 0 0 0;");

        root = new BorderPane();
        root.setTop(header);
        root.setCenter(buildLandingPane());
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 900, 560);
        stage.setTitle("Appointment Scheduler");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.show();
    }

    private void setHeaderSubtitleHome() {
        headerSubtitleLabel.setText("Home");
    }

    private void setHeaderSubtitleClientSearch() {
        headerSubtitleLabel.setText("Client Search");
    }

    private void setHeaderSubtitleAppointments() {
        headerSubtitleLabel.setText("Appointments");
    }

    private Button createNavButton(String text) {
        Button b = new Button(text);
        b.setMinWidth(BUTTON_MIN_WIDTH);
        return b;
    }

    private Button createHomeButton(String text) {
        Button b = createNavButton(text);
        b.setPrefWidth(175);
        b.setMinHeight(44);
        b.setPrefHeight(48);
        b.setWrapText(true);
        b.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        return b;
    }

    private static Label viewDetailValue(String text) {
        Label l = new Label(text == null ? "" : text);
        l.setStyle("-fx-text-fill: black;");
        l.setWrapText(true);
        l.setMaxWidth(460);
        return l;
    }

    private static String formatTime12h(LocalTime t) {
        int h24 = t.getHour();
        String amPm = h24 >= 12 ? "PM" : "AM";
        int h12 = h24 % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        return h12 + ":" + String.format("%02d", t.getMinute()) + " " + amPm;
    }

    private void onAddClient() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add client");
        dialog.setHeaderText("Enter client information");
        dialog.initOwner(primaryStage);

        ButtonType saveButtonType = new ButtonType("Save client", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("Required");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Required");

        ToggleGroup genderGroup = new ToggleGroup();
        RadioButton male = new RadioButton("Male");
        male.setToggleGroup(genderGroup);
        male.setUserData(Client.Gender.MALE);
        RadioButton female = new RadioButton("Female");
        female.setToggleGroup(genderGroup);
        female.setUserData(Client.Gender.FEMALE);
        RadioButton other = new RadioButton("Other");
        other.setToggleGroup(genderGroup);
        other.setUserData(Client.Gender.OTHER);
        other.setSelected(true);
        HBox genderRow = new HBox(12, male, female, other);

        TextField dobMonthField = digitField(2);
        TextField dobDayField = digitField(2);
        TextField dobYearField = digitField(4);
        HBox dobRow = new HBox(6, dobMonthField, new Label("/"), dobDayField, new Label("/"), dobYearField);
        dobRow.setAlignment(Pos.CENTER_LEFT);

        TextField phoneAreaField = digitField(3);
        TextField phonePrefixField = digitField(3);
        TextField phoneLineField = digitField(4);
        HBox phoneRow = new HBox(6, phoneAreaField, new Label("-"), phonePrefixField, new Label("-"), phoneLineField);
        phoneRow.setAlignment(Pos.CENTER_LEFT);

        TextField emailField = new TextField();
        emailField.setPromptText("Required");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 18, 10, 18));

        int r = 0;
        grid.add(new Label("First name:"), 0, r);
        grid.add(firstNameField, 1, r++);
        grid.add(new Label("Last name:"), 0, r);
        grid.add(lastNameField, 1, r++);
        grid.add(new Label("Gender:"), 0, r);
        grid.add(genderRow, 1, r++);
        grid.add(new Label("Date of birth:"), 0, r);
        grid.add(dobRow, 1, r++);
        grid.add(new Label("Email:"), 0, r);
        grid.add(emailField, 1, r++);
        grid.add(new Label("Phone:"), 0, r);
        grid.add(phoneRow, 1, r++);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(
                firstNameField.textProperty().isEmpty()
                        .or(lastNameField.textProperty().isEmpty())
                        .or(emailField.textProperty().isEmpty())
                        .or(dobMonthField.textProperty().isEmpty())
                        .or(dobDayField.textProperty().isEmpty())
                        .or(dobYearField.textProperty().isEmpty())
                        .or(phoneAreaField.textProperty().isEmpty())
                        .or(phonePrefixField.textProperty().isEmpty())
                        .or(phoneLineField.textProperty().isEmpty())
        );

        dialog.setResultConverter(buttonType -> buttonType);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveButtonType) {
            statusLabel.setText("Add client cancelled.");
            return;
        }

        try {
            Client.Gender gender = (Client.Gender) genderGroup.getSelectedToggle().getUserData();
            LocalDate dob = parseDobParts(dobMonthField.getText(), dobDayField.getText(), dobYearField.getText());
            String phone = formatPhone(phoneAreaField.getText(), phonePrefixField.getText(), phoneLineField.getText());
            Client created = clientService.addClient(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    gender,
                    dob,
                    emailField.getText(),
                    phone
            );
            statusLabel.setText("Client added: " + created.getDisplayName() + " (total clients: " + clientService.getAllClients().size() + ")");
            pendingSelectClientId = created.getId();
            showClientDashboard(false);
        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Could not add client: " + ex.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Add client failed");
            alert.setHeaderText("Please check the client information.");
            alert.setContentText(ex.getMessage());
            alert.initOwner(primaryStage);
            alert.showAndWait();
        }
    }

    private TableView<Client> buildClientsTable() {
        TableView<Client> table = new TableView<>();
        table.setPrefHeight(340);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Client, String> firstNameCol = new TableColumn<>("First");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        TableColumn<Client, String> lastNameCol = new TableColumn<>("Last");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));

        TableColumn<Client, Client.Gender> genderCol = new TableColumn<>("Gender");
        genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));

        TableColumn<Client, String> dobCol = new TableColumn<>("DOB");
        dobCol.setCellValueFactory(cell -> {
            LocalDate dob = cell.getValue().getDateOfBirth();
            return new javafx.beans.property.SimpleStringProperty(DOB_FORMAT.format(dob));
        });

        TableColumn<Client, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Client, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        table.getColumns().clear();
        table.getColumns().add(firstNameCol);
        table.getColumns().add(lastNameCol);
        table.getColumns().add(genderCol);
        table.getColumns().add(dobCol);
        table.getColumns().add(emailCol);
        table.getColumns().add(phoneCol);
        return table;
    }

    private void refreshClientTable() {
        String q = clientSearchField == null ? null : clientSearchField.getText();
        List<Client> results = clientService.searchClients(q);
        clientsTable.getItems().setAll(results);

        if (q == null || q.trim().isEmpty()) {
            statusLabel.setText("Clients loaded: " + results.size());
        } else {
            statusLabel.setText("Search results: " + results.size());
        }
    }

    private void onEditSelectedClient() {
        Client selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Edit client");
            alert.setHeaderText("No client selected");
            alert.setContentText("Select a client in the table first.");
            alert.initOwner(primaryStage);
            alert.showAndWait();
            return;
        }
        showEditClientDialog(selected);
    }

    private void onDeleteSelectedClient() {
        Client selected = clientsTable == null ? null : clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Delete client");
            alert.setHeaderText("No client selected");
            alert.setContentText("Select a client in the table first.");
            alert.initOwner(primaryStage);
            alert.showAndWait();
            return;
        }

        if (!appointmentService.getForClient(selected.getId()).isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Delete client");
            alert.setHeaderText("Client has appointments");
            alert.setContentText("Delete this client's appointments first, then delete the client.");
            alert.initOwner(primaryStage);
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete client");
        confirm.setHeaderText("Delete this client?");
        confirm.setContentText(selected.getDisplayName());
        confirm.initOwner(primaryStage);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean deleted = clientService.deleteClient(selected.getId());
        if (deleted) {
            statusLabel.setText("Client deleted.");
        } else {
            statusLabel.setText("Client not found (already deleted).");
        }
        refreshClientTable();
    }

    private void showEditClientDialog(Client existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit client");
        dialog.setHeaderText("Update client information");
        dialog.initOwner(primaryStage);

        ButtonType saveButtonType = new ButtonType("Save changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField firstNameField = new TextField(existing.getFirstName());
        TextField lastNameField = new TextField(existing.getLastName());

        ToggleGroup genderGroup = new ToggleGroup();
        RadioButton male = new RadioButton("Male");
        male.setToggleGroup(genderGroup);
        male.setUserData(Client.Gender.MALE);
        RadioButton female = new RadioButton("Female");
        female.setToggleGroup(genderGroup);
        female.setUserData(Client.Gender.FEMALE);
        RadioButton other = new RadioButton("Other");
        other.setToggleGroup(genderGroup);
        other.setUserData(Client.Gender.OTHER);
        HBox genderRow = new HBox(12, male, female, other);

        switch (existing.getGender()) {
            case MALE -> male.setSelected(true);
            case FEMALE -> female.setSelected(true);
            case OTHER -> other.setSelected(true);
        }

        TextField emailField = new TextField(existing.getEmail());
        String[] phoneParts = splitPhone(existing.getPhone());
        TextField phoneAreaField = digitField(3);
        TextField phonePrefixField = digitField(3);
        TextField phoneLineField = digitField(4);
        phoneAreaField.setText(phoneParts[0]);
        phonePrefixField.setText(phoneParts[1]);
        phoneLineField.setText(phoneParts[2]);
        HBox phoneRow = new HBox(6, phoneAreaField, new Label("-"), phonePrefixField, new Label("-"), phoneLineField);
        phoneRow.setAlignment(Pos.CENTER_LEFT);

        TextField dobMonthField = digitField(2);
        TextField dobDayField = digitField(2);
        TextField dobYearField = digitField(4);
        dobMonthField.setText(String.format("%02d", existing.getDateOfBirth().getMonthValue()));
        dobDayField.setText(String.format("%02d", existing.getDateOfBirth().getDayOfMonth()));
        dobYearField.setText(String.valueOf(existing.getDateOfBirth().getYear()));
        HBox dobRow = new HBox(6, dobMonthField, new Label("/"), dobDayField, new Label("/"), dobYearField);
        dobRow.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 18, 10, 18));

        int r = 0;
        grid.add(new Label("First name:"), 0, r);
        grid.add(firstNameField, 1, r++);
        grid.add(new Label("Last name:"), 0, r);
        grid.add(lastNameField, 1, r++);
        grid.add(new Label("Gender:"), 0, r);
        grid.add(genderRow, 1, r++);
        grid.add(new Label("Date of birth:"), 0, r);
        grid.add(dobRow, 1, r++);
        grid.add(new Label("Email:"), 0, r);
        grid.add(emailField, 1, r++);
        grid.add(new Label("Phone:"), 0, r);
        grid.add(phoneRow, 1, r++);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(
                firstNameField.textProperty().isEmpty()
                        .or(lastNameField.textProperty().isEmpty())
                        .or(emailField.textProperty().isEmpty())
                        .or(dobMonthField.textProperty().isEmpty())
                        .or(dobDayField.textProperty().isEmpty())
                        .or(dobYearField.textProperty().isEmpty())
                        .or(phoneAreaField.textProperty().isEmpty())
                        .or(phonePrefixField.textProperty().isEmpty())
                        .or(phoneLineField.textProperty().isEmpty())
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveButtonType) {
            statusLabel.setText("Edit client cancelled.");
            return;
        }

        try {
            Client.Gender gender = (Client.Gender) genderGroup.getSelectedToggle().getUserData();
            LocalDate dob = parseDobParts(dobMonthField.getText(), dobDayField.getText(), dobYearField.getText());
            String phone = formatPhone(phoneAreaField.getText(), phonePrefixField.getText(), phoneLineField.getText());
            Client updated = clientService.updateClient(
                    existing.getId(),
                    firstNameField.getText(),
                    lastNameField.getText(),
                    gender,
                    dob,
                    emailField.getText(),
                    phone
            );
            statusLabel.setText("Client updated: " + updated.getDisplayName());
            refreshClientTable();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Could not update client: " + ex.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Edit client failed");
            alert.setHeaderText("Please check the client information.");
            alert.setContentText(ex.getMessage());
            alert.initOwner(primaryStage);
            alert.showAndWait();
        }
    }

    private void onBookAppointmentForSelectedClient() {
        Client selected = clientsTable == null ? null : clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Book Appointment");
            alert.setHeaderText("No client selected");
            alert.setContentText("Select a client first.");
            alert.initOwner(primaryStage);
            alert.showAndWait();
            return;
        }
        showBookAppointmentDialog(selected);
    }

    private void onViewAppointmentsForSelectedClient() {
        Client selected = clientsTable == null ? null : clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Client appointments");
            alert.setHeaderText("No client selected");
            alert.setContentText("Select a client first.");
            alert.initOwner(primaryStage);
            alert.showAndWait();
            return;
        }
        showAppointmentsByClientScreen(selected);
    }

    private void showAppointmentsByClientScreen(Client client) {
        Objects.requireNonNull(client, "client");
        setHeaderSubtitleAppointments();

        apptsByClientClientId = client.getId();
        apptsByClientTable = buildAppointmentsTable();
        apptsByClientTable.getItems().setAll(appointmentService.getForClient(apptsByClientClientId));

        Label title = new Label("Appointments for " + client.getDisplayName());
        title.setFont(Font.font(null, FontWeight.BOLD, 16));
        title.setPadding(new Insets(10, 24, 0, 24));

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            pendingSelectClientId = client.getId();
            showClientDashboard(false);
            statusLabel.setText("Client Search");
        });
        HBox topRow = new HBox(10, backButton);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(0, 24, 0, 24));

        Button viewApptButton = createNavButton("View Appointment");
        viewApptButton.setOnAction(e -> onViewSelectedAppointmentFrom(apptsByClientTable));
        viewApptButton.disableProperty().bind(apptsByClientTable.getSelectionModel().selectedItemProperty().isNull());

        Button deleteApptButton = createNavButton("Delete Appointment");
        deleteApptButton.setOnAction(e -> onDeleteSelectedAppointmentFrom(apptsByClientTable));
        deleteApptButton.disableProperty().bind(apptsByClientTable.getSelectionModel().selectedItemProperty().isNull());

        HBox actions = new HBox(12, viewApptButton, deleteApptButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(0, 24, 14, 24));

        VBox box = new VBox(10, topRow, title, apptsByClientTable, actions);
        box.setPadding(new Insets(0, 0, 0, 0));
        root.setCenter(box);

        statusLabel.setText("Appointments for " + client.getDisplayName() + ": " + apptsByClientTable.getItems().size());
    }

    private void showBookAppointmentDialog(Client client) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Book Appointment");
        dialog.setHeaderText("Book an appointment for " + client.getDisplayName());
        dialog.initOwner(primaryStage);

        ButtonType bookButtonType = new ButtonType("Book", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(bookButtonType, ButtonType.CANCEL);

        TextField dateMonth = digitField(2);
        TextField dateDay = digitField(2);
        TextField dateYear = digitField(4);
        HBox dateRow = new HBox(6, dateMonth, new Label("/"), dateDay, new Label("/"), dateYear);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        TextField startHourField = digitField(2);
        TextField startMinField = digitField(2);
        javafx.scene.control.ComboBox<String> startAmPm = new javafx.scene.control.ComboBox<>();
        startAmPm.getItems().setAll("AM", "PM");
        startAmPm.setValue("AM");
        startAmPm.setPrefWidth(70);
        HBox startRow = new HBox(6, startHourField, new Label(":"), startMinField, startAmPm);
        startRow.setAlignment(Pos.CENTER_LEFT);

        TextField endHourField = digitField(2);
        TextField endMinField = digitField(2);
        javafx.scene.control.ComboBox<String> endAmPm = new javafx.scene.control.ComboBox<>();
        endAmPm.getItems().setAll("AM", "PM");
        endAmPm.setValue("AM");
        endAmPm.setPrefWidth(70);
        HBox endRow = new HBox(6, endHourField, new Label(":"), endMinField, endAmPm);
        endRow.setAlignment(Pos.CENTER_LEFT);

        TextField subjectField = new TextField();
        subjectField.setPromptText("e.g., Consultation");

        javafx.scene.control.TextArea notesArea = new javafx.scene.control.TextArea();
        notesArea.setPromptText("Optional notes...");
        notesArea.setPrefRowCount(4);
        notesArea.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 18, 10, 18));

        int r = 0;
        grid.add(new Label("Date:"), 0, r);
        grid.add(dateRow, 1, r++);
        grid.add(new Label("Start time (HH:MM):"), 0, r);
        grid.add(startRow, 1, r++);
        grid.add(new Label("End time (HH:MM):"), 0, r);
        grid.add(endRow, 1, r++);
        grid.add(new Label("Subject:"), 0, r);
        grid.add(subjectField, 1, r++);
        Label notesLabel = new Label("Notes:");
        grid.add(notesLabel, 0, r);
        GridPane.setValignment(notesLabel, VPos.TOP);
        grid.add(notesArea, 1, r);

        dialog.getDialogPane().setContent(grid);

        Button bookBtn = (Button) dialog.getDialogPane().lookupButton(bookButtonType);
        bookBtn.disableProperty().bind(
                dateMonth.textProperty().isEmpty()
                        .or(dateDay.textProperty().isEmpty())
                        .or(dateYear.textProperty().isEmpty())
                        .or(startHourField.textProperty().isEmpty())
                        .or(startMinField.textProperty().isEmpty())
                        .or(endHourField.textProperty().isEmpty())
                        .or(endMinField.textProperty().isEmpty())
                        .or(subjectField.textProperty().isEmpty())
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != bookButtonType) {
            statusLabel.setText("Booking cancelled.");
            return;
        }

        try {
            LocalDate date = parseDateParts(dateMonth.getText(), dateDay.getText(), dateYear.getText());
            LocalTime start = parseTimeParts12h(
                    startHourField.getText(),
                    startMinField.getText(),
                    startAmPm.getValue(),
                    "start"
            );
            LocalTime end = parseTimeParts12h(
                    endHourField.getText(),
                    endMinField.getText(),
                    endAmPm.getValue(),
                    "end"
            );

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("end time must be after start time");
            }
            if (date.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("date cannot be in the past");
            }
            if (date.isEqual(LocalDate.now()) && start.isBefore(LocalTime.now())) {
                throw new IllegalArgumentException("start time cannot be in the past");
            }

            Appointment appt = Appointment.createNew(
                    client.getId(),
                    date,
                    start,
                    end,
                    subjectField.getText(),
                    notesArea.getText()
            );
            appointmentService.add(appt);
            showAppointmentsByRangeScreen(date);
            statusLabel.setText("Appointment booked for " + client.getDisplayName() + " on " + date + " (" + start + "-" + end + ").");
        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Could not book appointment: " + ex.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Booking failed");
            alert.setHeaderText("Please check appointment details.");
            alert.setContentText(ex.getMessage());
            alert.initOwner(primaryStage);
            alert.showAndWait();
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private VBox buildLandingPane() {
        Button searchClientButton = createHomeButton("Client Search");
        searchClientButton.setOnAction(e -> showClientDashboard(false));

        Button addClientButton = createHomeButton("Add Client");
        addClientButton.setOnAction(e -> onAddClient());

        Button viewAppointmentsButton = createHomeButton("View Appointments");
        viewAppointmentsButton.setOnAction(e -> showAppointmentsByRangeScreen(LocalDate.now()));

        HBox row = new HBox(14, searchClientButton, addClientButton, viewAppointmentsButton);
        row.setAlignment(Pos.CENTER);

        VBox landing = new VBox(18, row);
        landing.setAlignment(Pos.CENTER);
        landing.setPadding(new Insets(12, 24, 12, 24));
        return landing;
    }

    private void showClientDashboard(boolean openEditHint) {
        setHeaderSubtitleClientSearch();
        clientSearchField = new TextField();
        clientSearchField.setPromptText("Search clients (name, email, phone)...");
        clientSearchField.setPrefWidth(420);

        Button searchButton = new Button("Search");
        searchButton.setOnAction(e -> refreshClientTable());

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> {
            clientSearchField.setText("");
            refreshClientTable();
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            root.setCenter(buildLandingPane());
            setHeaderSubtitleHome();
            statusLabel.setText("Please make a selection.");
        });

        HBox searchRow = new HBox(10, new Label("Search:"), clientSearchField, searchButton, clearButton, backButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(0, 24, 0, 24));

        clientsTable = buildClientsTable();

        Button addClientButton = createNavButton("Add Client");
        addClientButton.setOnAction(e -> onAddClient());

        Button editClientButton = createNavButton("Edit Client");
        editClientButton.setOnAction(e -> onEditSelectedClient());

        Button bookAppointmentButton = createNavButton("Book Appointment");
        bookAppointmentButton.setOnAction(e -> onBookAppointmentForSelectedClient());
        bookAppointmentButton.disableProperty().bind(clientsTable.getSelectionModel().selectedItemProperty().isNull());

        Button clientAppointmentsButton = createNavButton("Client Appointments");
        clientAppointmentsButton.setOnAction(e -> onViewAppointmentsForSelectedClient());
        clientAppointmentsButton.disableProperty().bind(clientsTable.getSelectionModel().selectedItemProperty().isNull());

        Button deleteClientButton = createNavButton("Delete Client");
        deleteClientButton.setOnAction(e -> onDeleteSelectedClient());
        deleteClientButton.disableProperty().bind(clientsTable.getSelectionModel().selectedItemProperty().isNull());

        HBox actionsRow = new HBox(12, addClientButton, editClientButton, clientAppointmentsButton, deleteClientButton, bookAppointmentButton);
        actionsRow.setAlignment(Pos.CENTER);
        actionsRow.setPadding(new Insets(8, 24, 8, 24));

        VBox dashboard = new VBox(14, searchRow, clientsTable, actionsRow);
        dashboard.setAlignment(Pos.TOP_CENTER);
        dashboard.setPadding(new Insets(0, 0, 8, 0));

        root.setCenter(dashboard);
        refreshClientTable();

        if (pendingSelectClientId != null) {
            selectClientInTable(pendingSelectClientId);
            pendingSelectClientId = null;
        }

        if (openEditHint) {
            statusLabel.setText("Select a client, then click Edit Client.");
        }
    }

    private void selectClientInTable(UUID clientId) {
        if (clientsTable == null || clientId == null) {
            return;
        }
        for (Client c : clientsTable.getItems()) {
            if (clientId.equals(c.getId())) {
                clientsTable.getSelectionModel().select(c);
                clientsTable.scrollTo(c);
                break;
            }
        }
    }

    private static TextField digitField(int maxDigits) {
        TextField tf = new TextField();
        tf.setPrefColumnCount(maxDigits);
        tf.textProperty().addListener((obs, oldV, newV) -> {
            String digitsOnly = newV == null ? "" : newV.replaceAll("\\D", "");
            if (digitsOnly.length() > maxDigits) {
                digitsOnly = digitsOnly.substring(0, maxDigits);
            }
            if (!digitsOnly.equals(newV)) {
                tf.setText(digitsOnly);
            }
        });
        return tf;
    }

    private static LocalDate parseDobParts(String mm, String dd, String yyyy) {
        String m = requireDigits(mm, 2, "DOB month");
        String d = requireDigits(dd, 2, "DOB day");
        String y = requireDigits(yyyy, 4, "DOB year");
        String combined = m + "/" + d + "/" + y;
        try {
            return LocalDate.parse(combined, DOB_FORMAT);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("dateOfBirth must be a real date (MM/DD/YYYY)");
        }
    }

    private static LocalDate parseDateParts(String mm, String dd, String yyyy) {
        String m = requireDigits(mm, 2, "month");
        String d = requireDigits(dd, 2, "day");
        String y = requireDigits(yyyy, 4, "year");
        String combined = m + "/" + d + "/" + y;
        try {
            return LocalDate.parse(combined, APPT_DATE_FORMAT);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("date must be a real date (MM/DD/YYYY)");
        }
    }

    private static String formatPhone(String area, String prefix, String line) {
        String a = requireDigits(area, 3, "phone area code");
        String p = requireDigits(prefix, 3, "phone prefix");
        String l = requireDigits(line, 4, "phone line number");
        return a + "-" + p + "-" + l;
    }

    private static String[] splitPhone(String phone) {
        if (phone == null) {
            return new String[]{"", "", ""};
        }
        String[] parts = phone.split("-");
        if (parts.length != 3) {
            return new String[]{"", "", ""};
        }
        return new String[]{parts[0], parts[1], parts[2]};
    }

    private static String requireDigits(String value, int exactLen, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != exactLen) {
            throw new IllegalArgumentException(fieldName + " must be " + exactLen + " digits");
        }
        return digits;
    }

    private void showAppointmentsByRangeScreen(LocalDate focusDate) {
        setHeaderSubtitleAppointments();
        seedExampleAppointmentsIfEmpty();
        if (focusDate != null) {
            appointmentsAnchorDate = focusDate;
        }
        VBox content = buildApptsByRangePane();
        root.setCenter(wrapWithBackButton(content));
        statusLabel.setText("Upcoming Appointments, by week.");
    }

    private VBox wrapWithBackButton(VBox content) {
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            root.setCenter(buildLandingPane());
            setHeaderSubtitleHome();
            statusLabel.setText("Please make a selection.");
        });
        HBox topRow = new HBox(backButton);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(0, 24, 0, 24));

        VBox layout = new VBox(12, topRow, content);
        layout.setPadding(new Insets(8, 0, 8, 0));
        return layout;
    }

    private VBox buildApptsByRangePane() {
        Button weekBackBtn = new Button("Back");
        Button weekNextBtn = new Button("Next");
        Button todayBtn = new Button("Today");

        appointmentsRangeLabel = new Label();

        apptsByRangeTable = buildAppointmentsTable();

        Runnable refreshWeek = () -> {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = AppointmentService.startOfWeek(appointmentsAnchorDate);
            LocalDate weekEnd = weekStart.plusDays(6);
            appointmentsRangeLabel.setText(
                    "Upcoming (from " + today + "): week " + weekStart + " to " + weekEnd
            );
            apptsByRangeTable.getItems().setAll(
                    appointmentService.getUpcomingForWeek(appointmentsAnchorDate, today)
            );
        };

        weekBackBtn.setOnAction(e -> {
            appointmentsAnchorDate = appointmentsAnchorDate.minusWeeks(1);
            refreshWeek.run();
        });
        weekNextBtn.setOnAction(e -> {
            appointmentsAnchorDate = appointmentsAnchorDate.plusWeeks(1);
            refreshWeek.run();
        });
        todayBtn.setOnAction(e -> {
            appointmentsAnchorDate = LocalDate.now();
            refreshWeek.run();
        });

        HBox controls = new HBox(10, weekBackBtn, weekNextBtn, todayBtn, appointmentsRangeLabel);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10, 24, 0, 24));

        Button viewApptButton = createNavButton("View Appointment");
        viewApptButton.setOnAction(e -> onViewSelectedAppointment());
        viewApptButton.disableProperty().bind(apptsByRangeTable.getSelectionModel().selectedItemProperty().isNull());

        Button deleteApptButton = createNavButton("Delete Appointment");
        deleteApptButton.setOnAction(e -> onDeleteSelectedAppointment());
        deleteApptButton.disableProperty().bind(apptsByRangeTable.getSelectionModel().selectedItemProperty().isNull());

        HBox actions = new HBox(12, viewApptButton, deleteApptButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(0, 24, 0, 24));

        refreshWeek.run();

        VBox box = new VBox(10, controls, apptsByRangeTable, actions);
        box.setPadding(new Insets(0, 24, 14, 24));
        return box;
    }

    private void onViewSelectedAppointment() {
        onViewSelectedAppointmentFrom(apptsByRangeTable);
    }

    private void onDeleteSelectedAppointment() {
        onDeleteSelectedAppointmentFrom(apptsByRangeTable);
    }

    private void onViewSelectedAppointmentFrom(TableView<Appointment> table) {
        Appointment selected = table == null ? null : table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        showViewAppointmentDialog(selected);
    }

    private void onDeleteSelectedAppointmentFrom(TableView<Appointment> table) {
        Appointment selected = table == null ? null : table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Appointment");
        confirm.setHeaderText("Delete this appointment?");
        confirm.setContentText(selected.getSubject() + " on " + selected.getDate() + " (" + selected.getStartTime() + "-" + selected.getEndTime() + ")");
        confirm.initOwner(primaryStage);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean deleted = appointmentService.delete(selected.getId());
        if (deleted) {
            statusLabel.setText("Appointment deleted.");
        } else {
            statusLabel.setText("Appointment not found (already deleted).");
        }
        refreshAppointmentsTableAfterChange();
    }

    private void showViewAppointmentDialog(Appointment existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("View Appointment");
        dialog.setHeaderText("Appointment details");
        dialog.initOwner(primaryStage);

        ButtonType editButtonType = new ButtonType("Edit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(editButtonType, ButtonType.CLOSE);

        Label subjectValue = viewDetailValue(existing.getSubject());
        Label dateValue = viewDetailValue(APPT_DATE_FORMAT.format(existing.getDate()));
        Label startValue = viewDetailValue(formatTime12h(existing.getStartTime()));
        Label endValue = viewDetailValue(formatTime12h(existing.getEndTime()));
        String notesText = existing.getNotes() == null || existing.getNotes().isBlank()
                ? "(none)"
                : existing.getNotes();
        Label notesValue = viewDetailValue(notesText);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 18, 10, 18));

        int r = 0;
        grid.add(new Label("Subject:"), 0, r);
        grid.add(subjectValue, 1, r++);
        grid.add(new Label("Date:"), 0, r);
        grid.add(dateValue, 1, r++);
        grid.add(new Label("Start time:"), 0, r);
        grid.add(startValue, 1, r++);
        grid.add(new Label("End time:"), 0, r);
        grid.add(endValue, 1, r++);
        Label notesLabel = new Label("Notes:");
        grid.add(notesLabel, 0, r);
        GridPane.setValignment(notesLabel, VPos.TOP);
        grid.add(notesValue, 1, r);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == editButtonType) {
            showEditAppointmentDialog(existing);
        }
    }

    private void showEditAppointmentDialog(Appointment existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Appointment");
        dialog.setHeaderText("Edit appointment details");
        dialog.initOwner(primaryStage);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField subjectField = new TextField(existing.getSubject());

        TextField dateMonth = digitField(2);
        TextField dateDay = digitField(2);
        TextField dateYear = digitField(4);
        dateMonth.setText(String.format("%02d", existing.getDate().getMonthValue()));
        dateDay.setText(String.format("%02d", existing.getDate().getDayOfMonth()));
        dateYear.setText(String.valueOf(existing.getDate().getYear()));
        HBox dateRow = new HBox(6, dateMonth, new Label("/"), dateDay, new Label("/"), dateYear);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        int startHour24 = existing.getStartTime().getHour();
        String startAmPmValue = startHour24 >= 12 ? "PM" : "AM";
        int startHour12 = startHour24 % 12;
        if (startHour12 == 0) {
            startHour12 = 12;
        }
        TextField startHourField = digitField(2);
        TextField startMinField = digitField(2);
        startHourField.setText(String.format("%02d", startHour12));
        startMinField.setText(String.format("%02d", existing.getStartTime().getMinute()));
        javafx.scene.control.ComboBox<String> startAmPm = new javafx.scene.control.ComboBox<>();
        startAmPm.getItems().setAll("AM", "PM");
        startAmPm.setPrefWidth(70);
        startAmPm.setValue(startAmPmValue);
        HBox startRow = new HBox(6, startHourField, new Label(":"), startMinField, startAmPm);
        startRow.setAlignment(Pos.CENTER_LEFT);

        int endHour24 = existing.getEndTime().getHour();
        String endAmPmValue = endHour24 >= 12 ? "PM" : "AM";
        int endHour12 = endHour24 % 12;
        if (endHour12 == 0) {
            endHour12 = 12;
        }
        TextField endHourField = digitField(2);
        TextField endMinField = digitField(2);
        endHourField.setText(String.format("%02d", endHour12));
        endMinField.setText(String.format("%02d", existing.getEndTime().getMinute()));
        javafx.scene.control.ComboBox<String> endAmPm = new javafx.scene.control.ComboBox<>();
        endAmPm.getItems().setAll("AM", "PM");
        endAmPm.setPrefWidth(70);
        endAmPm.setValue(endAmPmValue);
        HBox endRow = new HBox(6, endHourField, new Label(":"), endMinField, endAmPm);
        endRow.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.control.TextArea notesArea = new javafx.scene.control.TextArea();
        notesArea.setPromptText("Optional notes...");
        notesArea.setPrefRowCount(4);
        notesArea.setWrapText(true);
        notesArea.setText(existing.getNotes() == null ? "" : existing.getNotes());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(14, 18, 10, 18));

        int r = 0;
        grid.add(new Label("Subject:"), 0, r);
        grid.add(subjectField, 1, r++);
        grid.add(new Label("Date:"), 0, r);
        grid.add(dateRow, 1, r++);
        grid.add(new Label("Start time (HH:MM):"), 0, r);
        grid.add(startRow, 1, r++);
        grid.add(new Label("End time (HH:MM):"), 0, r);
        grid.add(endRow, 1, r++);
        Label notesLabel = new Label("Notes:");
        grid.add(notesLabel, 0, r);
        GridPane.setValignment(notesLabel, VPos.TOP);
        grid.add(notesArea, 1, r);

        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveBtn.disableProperty().bind(
                subjectField.textProperty().isEmpty()
                        .or(dateMonth.textProperty().isEmpty())
                        .or(dateDay.textProperty().isEmpty())
                        .or(dateYear.textProperty().isEmpty())
                        .or(startHourField.textProperty().isEmpty())
                        .or(startMinField.textProperty().isEmpty())
                        .or(endHourField.textProperty().isEmpty())
                        .or(endMinField.textProperty().isEmpty())
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveButtonType) {
            return;
        }

        try {
            LocalDate date = parseDateParts(dateMonth.getText(), dateDay.getText(), dateYear.getText());
            LocalTime start = parseTimeParts12h(startHourField.getText(), startMinField.getText(), startAmPm.getValue(), "start");
            LocalTime end = parseTimeParts12h(endHourField.getText(), endMinField.getText(), endAmPm.getValue(), "end");

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("end time must be after start time");
            }
            if (date.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("date cannot be in the past");
            }
            if (date.isEqual(LocalDate.now()) && start.isBefore(LocalTime.now())) {
                throw new IllegalArgumentException("start time cannot be in the past");
            }

            Appointment updated = new Appointment(
                    existing.getId(),
                    existing.getClientId(),
                    date,
                    start,
                    end,
                    subjectField.getText(),
                    notesArea.getText()
            );
            appointmentService.update(updated);
            statusLabel.setText("Appointment updated.");
            refreshAppointmentsTableAfterChange();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText("Could not update appointment: " + ex.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Edit failed");
            alert.setHeaderText("Please check appointment details.");
            alert.setContentText(ex.getMessage());
            alert.initOwner(primaryStage);
            alert.showAndWait();
        }
    }

    private void refreshAppointmentsTableAfterChange() {
        if (apptsByRangeTable != null && apptsByRangeTable.getItems() != null) {
            LocalDate today = LocalDate.now();
            apptsByRangeTable.getItems().setAll(
                    appointmentService.getUpcomingForWeek(appointmentsAnchorDate, today)
            );
            LocalDate weekStart = AppointmentService.startOfWeek(appointmentsAnchorDate);
            LocalDate weekEnd = weekStart.plusDays(6);
            if (appointmentsRangeLabel != null) {
                appointmentsRangeLabel.setText(
                        "Upcoming (from " + today + "): week " + weekStart + " to " + weekEnd
                );
            }
        }

        if (apptsByClientTable != null && apptsByClientTable.getItems() != null && apptsByClientClientId != null) {
            apptsByClientTable.getItems().setAll(
                    appointmentService.getForClient(apptsByClientClientId)
            );
        }
    }

    private static LocalTime parseTimeParts(String hh, String mm, String label) {
        int h = Integer.parseInt(requireDigits(hh, 2, label + " hour"));
        int m = Integer.parseInt(requireDigits(mm, 2, label + " minute"));
        if (h < 0 || h > 23) {
            throw new IllegalArgumentException(label + " hour must be 00-23");
        }
        if (m < 0 || m > 59) {
            throw new IllegalArgumentException(label + " minute must be 00-59");
        }
        return LocalTime.of(h, m);
    }

    private static LocalTime parseTimeParts12h(String hh, String mm, String amPm, String label) {
        if (amPm == null || (!amPm.equals("AM") && !amPm.equals("PM"))) {
            throw new IllegalArgumentException(label + " AM/PM is required");
        }
        int h12 = Integer.parseInt(requireDigits(hh, 2, label + " hour"));
        int m = Integer.parseInt(requireDigits(mm, 2, label + " minute"));
        if (h12 < 1 || h12 > 12) {
            throw new IllegalArgumentException(label + " hour must be 01-12");
        }
        if (m < 0 || m > 59) {
            throw new IllegalArgumentException(label + " minute must be 00-59");
        }
        int h24 = h12 % 12;
        if (amPm.equals("PM")) {
            h24 += 12;
        }
        return LocalTime.of(h24, m);
    }

    private TableView<Appointment> buildAppointmentsTable() {
        TableView<Appointment> table = new TableView<>();
        table.setPrefHeight(360);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Appointment, String> subjectCol = new TableColumn<>("Subject");
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subject"));

        TableColumn<Appointment, LocalTime> startCol = new TableColumn<>("Start");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));

        TableColumn<Appointment, LocalTime> endCol = new TableColumn<>("End");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        TableColumn<Appointment, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Appointment, String> clientCol = new TableColumn<>("Client");
        clientCol.setCellValueFactory(cell -> {
            UUID id = cell.getValue().getClientId();
            String name = clientService.findById(id).map(Client::getDisplayName).orElse("(unknown)");
            return new javafx.beans.property.SimpleStringProperty(name);
        });

        table.getColumns().clear();
        table.getColumns().add(subjectCol);
        table.getColumns().add(dateCol);
        table.getColumns().add(startCol);
        table.getColumns().add(endCol);
        table.getColumns().add(clientCol);
        return table;
    }

    private void seedExampleAppointmentsIfEmpty() {
        if (!appointmentService.getAll().isEmpty()) {
            return;
        }
        List<Client> clients = clientService.getAllClients();
        if (clients.isEmpty()) {
            return;
        }
        Client c0 = clients.get(0);
        LocalDate today = LocalDate.now();
        appointmentService.add(Appointment.createNew(c0.getId(), today, LocalTime.of(9, 0), LocalTime.of(9, 30), "Consultation", null));
        appointmentService.add(Appointment.createNew(c0.getId(), today.plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0), "Follow-up", "Bring paperwork"));
    }
}
