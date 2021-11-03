package eu.outerheaven.certmanager.controller.dto

import eu.outerheaven.certmanager.controller.form.KeystoreFormGUI

class CertificateImportDto {

        List<KeystoreFormGUI> selectedKeystores
        String password
        String filename
        String importFormat
        String base64File

}
