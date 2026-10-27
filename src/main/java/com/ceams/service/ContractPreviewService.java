package com.ceams.service;

import com.ceams.model.entity.StampApproval;
import java.io.File;

public interface ContractPreviewService {

    File generatePreviewWithStamps(StampApproval approval);

    void cleanupOldPreviews();

    File generateStampedContract(StampApproval approval);
}
