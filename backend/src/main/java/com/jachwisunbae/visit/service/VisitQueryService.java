package com.jachwisunbae.visit.service;

import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.common.page.PageResult;
import com.jachwisunbae.property.service.PropertyAccessService;
import com.jachwisunbae.visit.repository.VisitQueryRepository;
import com.jachwisunbae.visit.service.dto.result.VisitDetailResult;
import com.jachwisunbae.visit.service.dto.result.VisitListItemResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitQueryService {

    private final PropertyAccessService propertyAccessService;
    private final VisitQueryRepository visitQueryRepository;

    public VisitQueryService(
            final PropertyAccessService propertyAccessService,
            final VisitQueryRepository visitQueryRepository
    ) {
        this.propertyAccessService = propertyAccessService;
        this.visitQueryRepository = visitQueryRepository;
    }

    @Transactional(readOnly = true, timeout = 30)
    public PageResult<VisitListItemResult> getVisits(
            final long memberId,
            final long propertyId,
            final PageQuery pageQuery
    ) {
        propertyAccessService.requireOwned(memberId, propertyId);
        final long totalElements = visitQueryRepository.countOwnedByProperty(memberId, propertyId);
        final List<VisitListItemResult> content = totalElements == 0
                ? List.of()
                : visitQueryRepository.findAllOwnedByProperty(memberId, propertyId, pageQuery).stream()
                        .map(VisitListItemResult::from)
                        .toList();
        return PageResult.of(content, pageQuery, totalElements);
    }

    @Transactional(readOnly = true, timeout = 30)
    public VisitDetailResult getVisit(final long memberId, final long visitId) {
        final var rows = visitQueryRepository.findOwnedDetail(memberId, visitId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.VISIT_NOT_FOUND);
        }
        return VisitDetailResult.from(rows);
    }
}
