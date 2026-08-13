package com.jachwisunbae.property.service;

import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.time.DatabaseTime;
import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.domain.Property;
import com.jachwisunbae.property.domain.PropertyMemo;
import com.jachwisunbae.property.domain.PropertyName;
import com.jachwisunbae.property.domain.PropertyPreVisitMemo;
import com.jachwisunbae.property.domain.PreVisitMemoField;
import com.jachwisunbae.property.repository.PropertyPreVisitMemoRepository;
import com.jachwisunbae.property.repository.PropertyRepository;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import com.jachwisunbae.property.service.dto.command.SavePropertyMemoCommand;
import com.jachwisunbae.property.service.dto.command.UpdatePropertyCommand;
import com.jachwisunbae.property.service.dto.result.PropertyMemoResult;
import com.jachwisunbae.property.service.dto.result.PropertyResult;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyCommandService {

    private final PropertyRepository propertyRepository;
    private final PropertyPreVisitMemoRepository propertyPreVisitMemoRepository;
    private final Clock clock;

    public PropertyCommandService(
            final PropertyRepository propertyRepository,
            final PropertyPreVisitMemoRepository propertyPreVisitMemoRepository,
            final Clock clock
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyPreVisitMemoRepository = propertyPreVisitMemoRepository;
        this.clock = clock;
    }

    @Transactional(timeout = 30)
    public PropertyResult createProperty(final long memberId, final CreatePropertyCommand command) {
        final Instant now = DatabaseTime.normalize(clock.instant());
        final Property property = Property.create(
                memberId,
                new PropertyName(command.name()),
                new Money(command.depositAmount()),
                new Money(command.monthlyRentAmount()),
                DiscoverySource.from(command.discoverySource()),
                now
        );
        return PropertyResult.from(propertyRepository.save(property));
    }

    @Transactional(timeout = 30)
    public PropertyResult updateProperty(
            final long memberId,
            final long propertyId,
            final UpdatePropertyCommand command
    ) {
        final Property property = findOwnedForUpdate(memberId, propertyId);
        final Property changedProperty = property.updateBasicInfo(
                command.name().map(PropertyName::new).orElse(null),
                command.depositAmount().map(Money::new).orElse(null),
                command.monthlyRentAmount().map(Money::new).orElse(null),
                command.discoverySource().map(DiscoverySource::from).orElse(null),
                DatabaseTime.normalize(clock.instant())
        );
        if (!propertyRepository.updateBasicInfo(changedProperty)) {
            throw propertyNotFound();
        }
        return PropertyResult.from(changedProperty);
    }

    @Transactional(timeout = 30)
    public PropertyMemoResult saveMemo(
            final long memberId,
            final long propertyId,
            final SavePropertyMemoCommand command
    ) {
        final Property property = findOwnedForUpdate(memberId, propertyId);
        final Instant now = DatabaseTime.normalize(clock.instant());
        final PropertyPreVisitMemo changedMemo = command.isLegacy()
                ? changeLegacyMemo(memberId, property, command, now)
                : replaceMemo(command, now);
        final Property changedProperty = property.updateMemo(
                changedMemo.additionalMemo(),
                now
        );
        if (!propertyPreVisitMemoRepository.upsertOwned(memberId, propertyId, changedMemo)) {
            throw propertyNotFound();
        }
        if (!propertyRepository.updateMemo(changedProperty)) {
            throw propertyNotFound();
        }
        return PropertyMemoResult.from(changedMemo);
    }

    private PropertyPreVisitMemo changeLegacyMemo(
            final long memberId,
            final Property property,
            final SavePropertyMemoCommand command,
            final Instant now
    ) {
        final PropertyPreVisitMemo currentMemo = propertyPreVisitMemoRepository.findOwned(memberId, property.id())
                .orElseGet(() -> PropertyPreVisitMemo.fallback(property.memo(), property.memoUpdatedAt()));
        return currentMemo.updateAdditionalMemo(new PropertyMemo(command.additionalMemo()), now);
    }

    private PropertyPreVisitMemo replaceMemo(
            final SavePropertyMemoCommand command,
            final Instant now
    ) {
        return new PropertyPreVisitMemo(
                new PreVisitMemoField(command.viewingSchedule()),
                new PreVisitMemoField(command.moveInAvailability()),
                new PreVisitMemoField(command.provisionalDeposit()),
                new PreVisitMemoField(command.roomOptions()),
                new PreVisitMemoField(command.maintenanceAndUtilities()),
                new PreVisitMemoField(command.commuteTime()),
                new PreVisitMemoField(command.governmentSupport()),
                new PropertyMemo(command.additionalMemo()),
                now
        );
    }

    private Property findOwnedForUpdate(final long memberId, final long propertyId) {
        return propertyRepository.findOwnedByIdForUpdate(memberId, propertyId)
                .orElseThrow(this::propertyNotFound);
    }

    private ResourceNotFoundException propertyNotFound() {
        return new ResourceNotFoundException(ErrorCode.PROPERTY_NOT_FOUND);
    }
}
