/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.lib.gui.viewer.tools;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.geom.Point2;
import qupath.lib.gui.viewer.ModeWrapper;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathROIObject;
import qupath.lib.roi.PolygonROI;
import qupath.lib.roi.interfaces.ROI;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;

/**
 * The PolygonEditTool is used for edit Polygon points
 *
 * @author Anuradha G
 */
public class PolygonEditTool extends MoveTool {

    final static private Logger logger = LoggerFactory.getLogger(PolygonEditTool.class);


    public PolygonEditTool(final ModeWrapper modes) {
        super(modes);
    }


    @Override
    public void mousePressed(MouseEvent e) {
        PathObject pathObject = viewer.getSelectedObject();
        if (pathObject == null) {
            super.mousePressed(e);
            return;
        }


        boolean isLocked = ((PathAnnotationObject) pathObject).isLocked() || !checkIfActionValid(viewer.getSelectedObject());
        boolean eraser = isEraser(e);
        boolean append = isAppend(e);
        if (!isLocked && (pathObject.getROI() instanceof PolygonROI) && (eraser || append)) {
            return;
        }


        super.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        PathObject pathObject = viewer.getSelectedObject();
        if (pathObject == null) {
            super.mouseReleased(e);
            return;
        }


        boolean isLocked = ((PathAnnotationObject) pathObject).isLocked() || !checkIfActionValid(viewer.getSelectedObject());
        boolean eraser = isEraser(e);
        boolean append = isAppend(e);
        if (!isLocked && (pathObject.getROI() instanceof PolygonROI) && (eraser || append)) {

            PolygonROI polygonROI = (PolygonROI) pathObject.getROI();
            List<Point2> points = polygonROI.getVertices().getPoints();
            Point2D uiPoint = viewer.componentPointToImagePoint(e.getX(), e.getY(), null, true);
            Point2 newPoint = new Point2(uiPoint.getX(), uiPoint.getY());
            Point2 nextdoor = points.stream().reduce((x, y) -> x.distance(newPoint) < y.distance(newPoint) ? x : y).get();
            if (eraser) {


                System.out.println(nextdoor.distance(newPoint));
                if (nextdoor.distance(newPoint) < 5) {
                    points.remove(nextdoor);
                    ROI roiUpdated = new PolygonROI(points, polygonROI.getC(), polygonROI.getZ(), polygonROI.getT());
                    ((PathROIObject) pathObject).setROI(roiUpdated);
                    viewer.getHierarchy().fireObjectsChangedEvent(this, Collections.singleton(pathObject));
                    return;
                }


            } else {
                int index = points.indexOf(nextdoor);
                points.add(index + 1, newPoint);
                ROI roiUpdated = new PolygonROI(points, polygonROI.getC(), polygonROI.getZ(), polygonROI.getT());
                ((PathROIObject) pathObject).setROI(roiUpdated);
                viewer.getHierarchy().fireObjectsChangedEvent(this, Collections.singleton(pathObject));
                return;
            }
        }

        super.mouseReleased(e);


    }


    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);

        // We don't want to change a waiting cursor unnecessarily
        Cursor cursorType = viewer.getCursor();
        if (cursorType == Cursor.WAIT)
            return;

        // If we are already translating, we must need a move cursor
        if (viewer.getROIEditor().isTranslating()) {
            if (cursorType != Cursor.MOVE)
                viewer.setCursor(Cursor.MOVE);
            return;
        }

        PathObject pathObject = viewer.getSelectedObject();
        if (pathObject == null) {
            ensureCursorType(Cursor.HAND);
            return;
        }


        boolean isLocked = ((PathAnnotationObject) pathObject).isLocked() || !checkIfActionValid(viewer.getSelectedObject());
        boolean eraser = isEraser(e);
        boolean append = isAppend(e);
        if (!isLocked && (pathObject.getROI() instanceof PolygonROI) && (eraser || append)) {

            if (eraser) {

                PolygonROI polygonROI = (PolygonROI) pathObject.getROI();
                List<Point2> points = polygonROI.getVertices().getPoints();
                Point2D uiPoint = viewer.componentPointToImagePoint(e.getX(), e.getY(), null, true);
                Point2 newPoint = new Point2(uiPoint.getX(), uiPoint.getY());
                Point2 nextdoor = points.stream().reduce((x, y) -> x.distance(newPoint) < y.distance(newPoint) ? x : y).get();
                if (nextdoor.distance(newPoint) < 5) {
                    ensureCursorType(Cursor.DISAPPEAR);
                    return;
                }


            } else {
                ensureCursorType(Cursor.CROSSHAIR);
                return;
            }
        }


        ensureCursorType(Cursor.HAND);
    }

    private boolean isEraser(MouseEvent e) {
        return e.isShiftDown();
    }

    private boolean isAppend(MouseEvent e) {
        return e.isAltDown();
    }


}
