package eu.outerheaven.certmanager.controller.form

import groovy.transform.ToString

@ToString(includeFields = true)
class PayloadUploadForm {

    String name

    String base64file

    List<PayloadLocationFormGUI> payloadLocationFormGUIS

}
