package com.docusign.docusign.domain;
public enum AuditAction {
    DOCUMENT_SENT,          // sender created & sent signature request
    SIGNER_VIEWED,          // signer fetched their pending requests
    SIGNER_SIGNED,          // signer signed the document
    SIGNER_DECLINED,        // signer declined the document
    REQUEST_COMPLETED       // all signers signed → document completed
}