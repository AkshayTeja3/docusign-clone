package com.docusign.docusign.event;

import java.util.UUID;

public record DocumentSignedEvent(UUID requestId,UUID signerId){}

