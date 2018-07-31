package qupath.wsival.commands;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.interfaces.PathCommand;
import qupath.lib.gui.helpers.DisplayHelpers;
import qupath.lib.images.ImageData;
import qupath.lib.projects.ProjectIO;
import qupath.lib.projects.ProjectImageEntry;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportWSICommand implements PathCommand {

    private QuPathGUI qupath;

    public ExportWSICommand(QuPathGUI qupath) {
        this.qupath = qupath;
    }

    private void saveValidatedMeta(ProjectImageEntry<BufferedImage> wsi) {
        QuPathGUI.UserProfileChoice userChoice = qupath.getUserProfileChoice();
        Map<String, String> meta = new HashMap<>(wsi.getMetadataMap());

        meta.put(QuPathGUI.WSI_VALIDATED, userChoice.name());
        ProjectImageEntry<BufferedImage> entry = new ProjectImageEntry<>(qupath.getProject(),
                wsi.getServerPath(), wsi.getImageName(), meta);

        qupath.getProject().removeImage(wsi);
        qupath.getProject().addImage(entry);
        qupath.refreshProject();
        ProjectIO.writeProject(qupath.getProject(), message -> DisplayHelpers.showErrorMessage("Error", message));
    }

    private void reassignValidationOwnership(ProjectImageEntry<BufferedImage> wsi, QuPathGUI.UserProfileChoice user) {
        Map<String, String> meta = new HashMap<>(wsi.getMetadataMap());

        if (user == null) {
            meta.remove(QuPathGUI.WSI_VALIDATED);
        } else {
            meta.put(QuPathGUI.WSI_VALIDATED, user.name());
        }
        ProjectImageEntry<BufferedImage> entry = new ProjectImageEntry<>(qupath.getProject(),
                wsi.getServerPath(), wsi.getImageName(), meta);

        qupath.getProject().removeImage(wsi);
        qupath.getProject().addImage(entry);
        qupath.refreshProject();
        ProjectIO.writeProject(qupath.getProject(), message -> DisplayHelpers.showErrorMessage("Error", message));
    }


    private boolean getConfirmation() {
        return DisplayHelpers.showConfirmDialog("Warning", "Warning: Once you validate a WSI it will " +
                "be send for review and you won't be able to edit it anymore. Proceed?");
    }

    @Override
    public void run() {
        ProjectImageEntry<BufferedImage> wsi = qupath.getProject().getImageEntry(qupath.getImageData().getServerPath());

        if (QuPathGUI.getInstance().getUserProfileChoice() == QuPathGUI.UserProfileChoice.ADMIN_MODE) {
            String[] choices = {"Reset validation",
                    "Give to " + QuPathGUI.UserProfileChoice.SPECIALIST_MODE,
                    "Give to " + QuPathGUI.UserProfileChoice.CONTRACTOR_MODE,
                    "Give to " + QuPathGUI.UserProfileChoice.REVIEWER_MODE};
            String curVal = wsi.getMetadataMap().get(QuPathGUI.WSI_VALIDATED) == null ? "No one" :
                    wsi.getMetadataMap().get(QuPathGUI.WSI_VALIDATED);
            String choice = DisplayHelpers.showChoiceDialog("Change validation ownership",
                    "In admin mode the validation button allow you to reset the \nWSI \"validation\" " +
                            "to another user profile.\nPlease select the profile to which you want to give " +
                            "the validation attribute.\n\nCurrently validated by: "
                            + curVal, choices, choices[0]);
            if (choice != null) {
                if (choice.equals(choices[0])) {
                    reassignValidationOwnership(wsi, null);
                } else if (choice.equals(choices[1])) {
                    reassignValidationOwnership(wsi, QuPathGUI.UserProfileChoice.SPECIALIST_MODE);
                } else if (choice.equals(choices[2])) {
                    reassignValidationOwnership(wsi, QuPathGUI.UserProfileChoice.CONTRACTOR_MODE);
                } else if (choice.equals(choices[3])) {
                    reassignValidationOwnership(wsi, QuPathGUI.UserProfileChoice.REVIEWER_MODE);
                }
            }
        } else {
            if (getConfirmation()) {
                saveValidatedMeta(wsi);
            }
        }
    }
}