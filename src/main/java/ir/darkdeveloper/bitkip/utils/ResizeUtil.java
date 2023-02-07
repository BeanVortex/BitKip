package ir.darkdeveloper.bitkip.utils;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ResizeUtil {

    public static void addResizeListener(Stage stage) {
        var resizeListener = new ResizeListener(stage);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_MOVED, resizeListener);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED, resizeListener);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_DRAGGED, resizeListener);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_EXITED, resizeListener);
        stage.getScene().addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, resizeListener);
        var children = stage.getScene().getRoot().getChildrenUnmodifiable();
        children.forEach(child -> addListenerDeeply(child, resizeListener));
    }

    public static void addListenerDeeply(Node node, EventHandler<MouseEvent> listener) {
        node.addEventHandler(MouseEvent.MOUSE_MOVED, listener);
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, listener);
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, listener);
        node.addEventHandler(MouseEvent.MOUSE_EXITED, listener);
        node.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, listener);
        if (node instanceof Parent parent) {
            var children = parent.getChildrenUnmodifiable();
            children.forEach(child -> addListenerDeeply(child, listener));
        }
    }

    static class ResizeListener implements EventHandler<MouseEvent> {
        private final Stage stage;
        private Cursor cursorEvent = Cursor.DEFAULT;
        private double startX = 0;
        private double startY = 0;

        public ResizeListener(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void handle(MouseEvent mouseEvent) {
            int border = 4;

            var mouseEventType = mouseEvent.getEventType();
            var scene = stage.getScene();

            double mouseEventX = mouseEvent.getSceneX(),
                    mouseEventY = mouseEvent.getSceneY(),
                    sceneWidth = scene.getWidth(),
                    sceneHeight = scene.getHeight();

            if (MouseEvent.MOUSE_MOVED.equals(mouseEventType)) {
                if (mouseEventX < border && mouseEventY < border) {
                    cursorEvent = Cursor.NW_RESIZE;
                } else if (mouseEventX < border && mouseEventY > sceneHeight - border) {
                    cursorEvent = Cursor.SW_RESIZE;
                } else if (mouseEventX > sceneWidth - border && mouseEventY < border) {
                    cursorEvent = Cursor.NE_RESIZE;
                } else if (mouseEventX > sceneWidth - border && mouseEventY > sceneHeight - border) {
                    cursorEvent = Cursor.SE_RESIZE;
                } else if (mouseEventX < border) {
                    cursorEvent = Cursor.W_RESIZE;
                } else if (mouseEventX > sceneWidth - border) {
                    cursorEvent = Cursor.E_RESIZE;
                } else if (mouseEventY < border) {
                    cursorEvent = Cursor.N_RESIZE;
                } else if (mouseEventY > sceneHeight - border) {
                    cursorEvent = Cursor.S_RESIZE;
                } else {
                    cursorEvent = Cursor.DEFAULT;
                }
                scene.setCursor(cursorEvent);
            } else if (MouseEvent.MOUSE_EXITED.equals(mouseEventType) || MouseEvent.MOUSE_EXITED_TARGET.equals(mouseEventType)) {
                scene.setCursor(Cursor.DEFAULT);
            } else if (MouseEvent.MOUSE_PRESSED.equals(mouseEventType)) {
                startX = stage.getWidth() - mouseEventX;
                startY = stage.getHeight() - mouseEventY;
            } else if (MouseEvent.MOUSE_DRAGGED.equals(mouseEventType)) {
                if (!Cursor.DEFAULT.equals(cursorEvent)) {
                    if (!Cursor.W_RESIZE.equals(cursorEvent) && !Cursor.E_RESIZE.equals(cursorEvent)) {
                        double minHeight = stage.getMinHeight() > (border * 2) ? stage.getMinHeight() : (border * 2);
                        if (Cursor.NW_RESIZE.equals(cursorEvent) || Cursor.N_RESIZE.equals(cursorEvent) || Cursor.NE_RESIZE.equals(cursorEvent)) {
                            var prefHeight = stage.getY() - mouseEvent.getScreenY() + stage.getHeight();
                            var a = stage.getHeight() >= minHeight && prefHeight <= stage.getMaxHeight();
                            var b = mouseEventY < 0 && prefHeight >= stage.getMinHeight() && prefHeight <= stage.getMaxHeight();
                            if (a || b) {
                                stage.setHeight(prefHeight);
                                stage.setY(mouseEvent.getScreenY());
                            }
                        } else {
                            var prefHeight = mouseEventY + startY;
                            var a = stage.getHeight() > minHeight && prefHeight <= stage.getMaxHeight();
                            var b = (prefHeight - stage.getHeight() > 0 && prefHeight <= stage.getMaxHeight());
                            if (a || b)
                                stage.setHeight(prefHeight);
                        }
                    }

                    if (!Cursor.N_RESIZE.equals(cursorEvent) && !Cursor.S_RESIZE.equals(cursorEvent)) {
                        double minWidth = stage.getMinWidth() > (border * 2) ? stage.getMinWidth() : (border * 2);
                        if (Cursor.NW_RESIZE.equals(cursorEvent) || Cursor.W_RESIZE.equals(cursorEvent) || Cursor.SW_RESIZE.equals(cursorEvent)) {
                            var prefWidth = stage.getX() - mouseEvent.getScreenX() + stage.getWidth();
                            if (stage.getMaxWidth() == 0)
                                stage.setMaxWidth(prefWidth + 1);
                            var a = stage.getWidth() > minWidth && prefWidth <= stage.getMaxWidth();
                            var b = mouseEventX < 0 && prefWidth >= stage.getMinWidth();
                            if (a || b) {
                                stage.setWidth(prefWidth);
                                stage.setX(mouseEvent.getScreenX());
                            }
                        } else {
                            var prefWidth = mouseEventX + startX;
                            if (stage.getMaxWidth() == 0)
                                stage.setMaxWidth(prefWidth + 1);
                            var a = stage.getWidth() >= minWidth && prefWidth <= stage.getMaxWidth();
                            var b = prefWidth - stage.getWidth() > 0 && prefWidth >= stage.getMinHeight();
                            if (a || b)
                                stage.setWidth(prefWidth);
                        }
                    }
                }

            }
        }
    }
}