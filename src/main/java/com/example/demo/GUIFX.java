package com.example.demo;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;

public class GUIFX extends Application {
    private TextField nombre;
    private TextField direccion;
    private TextField telefono;
    private Button altaBoton, bajaBoton, modifBoton, agregarTel, agregarDir;
    private TableView<Usuario> tabla;
    private ObservableList<Usuario> listaUsuarios;

    private static final String URL = "jdbc:mariadb://localhost:3307/agenda";
    private static final String USER = "usuario1";
    private static final String PASSWORD = "superpassword";

    @Override
    public void start(Stage stage) {
        BorderPane panelPrincipal = new BorderPane();

        GridPane panelOpciones = new GridPane();
        panelOpciones.setHgap(10);
        panelOpciones.setVgap(10);
        panelOpciones.setPadding(new Insets(10));

        nombre = new TextField();
        direccion = new TextField();
        telefono = new TextField();

        panelOpciones.add(new Label("Nombre:"), 0, 0);
        panelOpciones.add(nombre, 1, 0);
        panelOpciones.add(new Label("Dirección:"), 0, 1);
        panelOpciones.add(direccion, 1, 1);
        panelOpciones.add(new Label("Teléfono:"), 0, 2);
        panelOpciones.add(telefono, 1, 2);

        altaBoton = new Button("Alta");
        altaBoton.setDisable(true);
        panelOpciones.add(altaBoton, 1, 3);

        nombre.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());
        direccion.textProperty().addListener((obs, oldVal, newVal) -> validarCampos());

        VBox panelUsuarios = new VBox(10);
        panelUsuarios.setPadding(new Insets(10));

        HBox panelBotones = new HBox(10);
        bajaBoton = new Button("Baja");
        modifBoton = new Button("Modificar Usuario");
        agregarTel = new Button("Agregar Teléfono");
        agregarDir = new Button("Agregar Dirección");
        panelBotones.getChildren().addAll(bajaBoton, modifBoton, agregarTel, agregarDir);

        tabla = new TableView<>();
        listaUsuarios = FXCollections.observableArrayList();

        TableColumn<Usuario, String> colId = new TableColumn<>("Id");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Usuario, String> colPersonaId = new TableColumn<>("Persona Id");
        colPersonaId.setCellValueFactory(new PropertyValueFactory<>("personaId"));

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        TableColumn<Usuario, String> colDireccion = new TableColumn<>("Dirección");
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        TableColumn<Usuario, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        tabla.getColumns().addAll(colId, colPersonaId, colNombre, colDireccion, colTelefono);
        tabla.setItems(listaUsuarios);

        panelUsuarios.getChildren().addAll(panelBotones, tabla);

        panelPrincipal.setLeft(panelOpciones);
        panelPrincipal.setCenter(panelUsuarios);

        altaBoton.setOnAction(e -> altaUsuario());
        bajaBoton.setOnAction(e -> bajaUsuario());
        modifBoton.setOnAction(e -> modificarUsuario(stage));
        agregarTel.setOnAction(e -> agregarTelefono(stage));
        agregarDir.setOnAction(e -> agregarDireccion(stage));

        agregarUsuariosTabla();

        Scene scene = new Scene(panelPrincipal, 1000, 500);
        stage.setTitle("CRUD JavaFX");
        stage.setScene(scene);
        stage.show();
    }

    private void validarCampos() {
        boolean nombreOk = nombre.getText().trim().matches(".*\\b\\w{3,}\\b.*");
        boolean direccionOk = direccion.getText().trim().matches(".*\\b\\w{3,}\\b.*");
        altaBoton.setDisable(!(nombreOk && direccionOk));
    }

    private void altaUsuario() {
        String nuevoUsuario = "INSERT INTO Personas (nombre) VALUES (?)";
        try (Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conexion.prepareStatement(nuevoUsuario, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre.getText());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long personaId = generatedKeys.getLong(1);

                    String sqlDir = "INSERT INTO Direcciones (personaId, direccion) VALUES (?, ?)";
                    try (PreparedStatement psDir = conexion.prepareStatement(sqlDir)) {
                        psDir.setLong(1, personaId);
                        psDir.setString(2, direccion.getText());
                        psDir.executeUpdate();
                    }

                    String sqlTel = "INSERT INTO Telefonos (personaId, telefono) VALUES (?, ?)";
                    try (PreparedStatement psTel = conexion.prepareStatement(sqlTel)) {
                        psTel.setLong(1, personaId);
                        psTel.setString(2, telefono.getText());
                        psTel.executeUpdate();
                    }
                }
            }
            agregarUsuariosTabla();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        nombre.clear();
        direccion.clear();
        telefono.clear();
    }

    private void bajaUsuario() {
        Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            String sql = "DELETE FROM Personas WHERE id = ?";
            try (Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(seleccionado.getId()));
                ps.executeUpdate();
                agregarUsuariosTabla();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } else {
            new Alert(Alert.AlertType.WARNING, "Selecciona un usuario para eliminar").showAndWait();
        }
    }

    private void modificarUsuario(Stage owner) {
        Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona un usuario para modificar").showAndWait();
            return;
        }

        Stage ventanaModif = new Stage();
        ventanaModif.initModality(Modality.APPLICATION_MODAL);
        ventanaModif.initOwner(owner);
        ventanaModif.setTitle("Modificar Usuario");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField campoNombre = new TextField(seleccionado.getNombre());
        TextField campoDireccion = new TextField(seleccionado.getDireccion());
        TextField campoTelefono = new TextField(seleccionado.getTelefono());

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(campoNombre, 1, 0);
        grid.add(new Label("Dirección:"), 0, 1);
        grid.add(campoDireccion, 1, 1);
        grid.add(new Label("Teléfono:"), 0, 2);
        grid.add(campoTelefono, 1, 2);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        Button guardar = new Button("Guardar");
        Button cancelar = new Button("Cancelar");
        botones.getChildren().addAll(guardar, cancelar);

        VBox layout = new VBox(10, grid, botones);
        layout.setPadding(new Insets(10));

        guardar.setOnAction(ev -> {
            String nombreNuevo = campoNombre.getText();
            String direccionNueva = campoDireccion.getText();
            String telNuevo = campoTelefono.getText();

            try (Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD)) {

                try (PreparedStatement ps = conexion.prepareStatement("UPDATE Personas SET nombre=? WHERE id=?")) {
                    ps.setString(1, nombreNuevo);
                    ps.setInt(2, Integer.parseInt(seleccionado.getId()));
                    ps.executeUpdate();
                }

                try (PreparedStatement psDelDir = conexion.prepareStatement("DELETE FROM Direcciones WHERE personaId=?")) {
                    psDelDir.setInt(1, Integer.parseInt(seleccionado.getId()));
                    psDelDir.executeUpdate();
                }
                if (!campoDireccion.getText().trim().isEmpty()) {
                    String[] direcciones = campoDireccion.getText().split(",");
                    for (String dir : direcciones) {
                        dir = dir.trim();
                        if (!dir.isEmpty()) {
                            String sqlInsertDir = "INSERT INTO Direcciones (personaId, direccion) VALUES (?, ?)";
                            try (PreparedStatement psDir = conexion.prepareStatement(sqlInsertDir)) {
                                psDir.setInt(1, Integer.parseInt(seleccionado.getId()));
                                psDir.setString(2, dir);
                                psDir.executeUpdate();
                            }
                        }
                    }
                }

                try (PreparedStatement psDelTel = conexion.prepareStatement("DELETE FROM Telefonos WHERE personaId=?")) {
                    psDelTel.setInt(1, Integer.parseInt(seleccionado.getId()));
                    psDelTel.executeUpdate();
                }
                if (!telNuevo.trim().isEmpty()) {
                    try (PreparedStatement psTel = conexion.prepareStatement("INSERT INTO Telefonos (personaId, telefono) VALUES (?, ?)")) {
                        psTel.setInt(1, Integer.parseInt(seleccionado.getId()));
                        psTel.setString(2, telNuevo);
                        psTel.executeUpdate();
                    }
                }
                new Alert(Alert.AlertType.INFORMATION, "Usuario actualizado correctamente").showAndWait();
                agregarUsuariosTabla();
                ventanaModif.close();

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        cancelar.setOnAction(ev -> ventanaModif.close());

        ventanaModif.setScene(new Scene(layout));
        ventanaModif.showAndWait();
    }

    private void agregarTelefono(Stage si){
        Usuario usuarioSelect = tabla.getSelectionModel().getSelectedItem();

        if(usuarioSelect == null){
            new Alert(Alert.AlertType.WARNING, "Selecciona un usuario para modificar").showAndWait();
            return;
        }

        Stage ventanaModif = new Stage();
        ventanaModif.initModality(Modality.APPLICATION_MODAL);
        ventanaModif.initOwner(si);
        ventanaModif.setTitle("Agregar Teléfono");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField campoTelefono = new TextField();

        grid.add(new Label("Teléfono:"), 0, 2);
        grid.add(campoTelefono, 1, 2);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        Button guardar = new Button("Guardar");
        Button cancelar = new Button("Cancelar");
        botones.getChildren().addAll(guardar, cancelar);

        VBox layout = new VBox(10, grid, botones);
        layout.setPadding(new Insets(10));

        guardar.setOnAction(e -> {
            String sqlTel = "INSERT INTO Telefonos (personaId, telefono) VALUES (?,?)";

            try (Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement psTel = conexion.prepareStatement(sqlTel)) {

                psTel.setInt(1, Integer.parseInt(usuarioSelect.getId()));
                psTel.setString(2, campoTelefono.getText());
                psTel.executeUpdate();

                new Alert(Alert.AlertType.INFORMATION, "Teléfono agregado correctamente").showAndWait();
                agregarUsuariosTabla();
                ventanaModif.close();

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        cancelar.setOnAction(ev -> ventanaModif.close());
        ventanaModif.setScene(new Scene(layout));
        ventanaModif.showAndWait();
    }

    private void agregarDireccion(Stage si){
        Usuario usuarioSelect = tabla.getSelectionModel().getSelectedItem();

        if(usuarioSelect == null){
            new Alert(Alert.AlertType.WARNING, "Selecciona un usuario para modificar").showAndWait();
            return;
        }

        Stage ventanaModif = new Stage();
        ventanaModif.initModality(Modality.APPLICATION_MODAL);
        ventanaModif.initOwner(si);
        ventanaModif.setTitle("Agregar Dirección");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField campoDir = new TextField();

        grid.add(new Label("Dirección:"), 0, 2);
        grid.add(campoDir, 1, 2);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        Button guardar = new Button("Guardar");
        Button cancelar = new Button("Cancelar");
        botones.getChildren().addAll(guardar, cancelar);

        VBox layout = new VBox(10, grid, botones);
        layout.setPadding(new Insets(10));

        guardar.setOnAction(e -> {
            String sqlTel = "INSERT INTO direcciones (personaId, direccion) VALUES (?,?)";

            try (Connection conexion = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement psDir = conexion.prepareStatement(sqlTel)) {

                psDir.setInt(1, Integer.parseInt(usuarioSelect.getId()));
                psDir.setString(2, campoDir.getText());
                psDir.executeUpdate();

                new Alert(Alert.AlertType.INFORMATION, "Dirección agregada correctamente").showAndWait();
                agregarUsuariosTabla();
                ventanaModif.close();

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        cancelar.setOnAction(ev -> ventanaModif.close());
        ventanaModif.setScene(new Scene(layout));
        ventanaModif.showAndWait();
    }

    private void agregarUsuariosTabla() {
        listaUsuarios.clear();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Personas")) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");

                Statement stmtDir = conn.createStatement();
                ResultSet rsDir = stmtDir.executeQuery("SELECT direccion FROM direcciones WHERE personaId = " + id);
                StringBuilder direcciones = new StringBuilder();
                while (rsDir.next()) {
                    if (direcciones.length() > 0) direcciones.append(", ");
                    direcciones.append(rsDir.getString("direccion"));
                }
                rsDir.close();
                stmtDir.close();

                Statement stmtTel = conn.createStatement();
                ResultSet rsTel = stmtTel.executeQuery("SELECT telefono FROM Telefonos WHERE personaId = " + id);

                StringBuilder telefonos = new StringBuilder();
                while (rsTel.next()) {
                    if (telefonos.length() > 0) telefonos.append(", ");
                    telefonos.append(rsTel.getString("telefono"));
                }
                rsTel.close();
                stmtTel.close();

                listaUsuarios.add(new Usuario(
                        String.valueOf(id),
                        String.valueOf(id),
                        nombre,
                        direcciones.toString(),
                        telefonos.toString()
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static class Usuario {
        private final SimpleStringProperty id;
        private final SimpleStringProperty personaId;
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty direccion;
        private final SimpleStringProperty telefono;

        public Usuario(String id, String personaId, String nombre, String direccion, String telefono) {
            this.id = new SimpleStringProperty(id);
            this.personaId = new SimpleStringProperty(personaId);
            this.nombre = new SimpleStringProperty(nombre);
            this.direccion = new SimpleStringProperty(direccion);
            this.telefono = new SimpleStringProperty(telefono);
        }

        public String getId() { return id.get(); }
        public String getPersonaId() { return personaId.get(); }
        public String getNombre() { return nombre.get(); }
        public String getDireccion() { return direccion.get(); }
        public String getTelefono() { return telefono.get(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

