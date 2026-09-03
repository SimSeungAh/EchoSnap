package com.echosnap.backend.domain.residence.entity;

import com.echosnap.backend.domain.collectionarea.entity.CollectionArea;
import com.echosnap.backend.domain.collectionarea.entity.CollectionWasteType;
import com.echosnap.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "residence_collection_areas",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_residence_collection_area_waste_type",
            columnNames = {
                "residence_id",
                "waste_type"
            }
        )
    },
    indexes = {
        @Index(
            name = "idx_residence_collection_areas_residence",
            columnList = "residence_id"
        ),
        @Index(
            name = "idx_residence_collection_areas_area",
            columnList = "collection_area_id"
        ),
        @Index(
            name = "idx_residence_collection_areas_waste_type",
            columnList = "waste_type"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResidenceCollectionArea extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "residence_id",
      nullable = false
  )
  private Residence residence;

  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "collection_area_id",
      nullable = false
  )
  private CollectionArea collectionArea;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "waste_type",
      nullable = false,
      length = 30
  )
  private CollectionWasteType wasteType;

  private ResidenceCollectionArea(
      Residence residence,
      CollectionArea collectionArea,
      CollectionWasteType wasteType
  ) {
    this.residence = residence;
    this.collectionArea = collectionArea;
    this.wasteType = wasteType;
  }

  public static ResidenceCollectionArea create(
      Residence residence,
      CollectionArea collectionArea,
      CollectionWasteType wasteType
  ) {
    return new ResidenceCollectionArea(
        residence,
        collectionArea,
        wasteType
    );
  }

  public void changeCollectionArea(
      CollectionArea collectionArea
  ) {
    this.collectionArea = collectionArea;
  }
}