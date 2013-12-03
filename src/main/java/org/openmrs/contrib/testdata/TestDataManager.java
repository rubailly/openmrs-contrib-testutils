package org.openmrs.contrib.testdata;

import static org.databene.benerator.util.SimpleRandom.randomInt;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.contrib.testdata.builder.EncounterBuilder;
import org.openmrs.contrib.testdata.builder.ObsBuilder;
import org.openmrs.contrib.testdata.builder.PatientBuilder;
import org.openmrs.contrib.testdata.builder.ProviderBuilder;
import org.openmrs.contrib.testdata.builder.TestDataBuilder;
import org.openmrs.contrib.testdata.builder.VisitBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class TestDataManager {

    private static final String[] GENDERS = {"M", "F"};

    private static final String[] MALE_FIRST_NAMES = { "James", "John", "Robert", "Michael", "William", "David", "Richard",
            "Joseph", "Charles", "Thomas", "Christopher", "Daniel", "Matthew", "Donald", "Anthony", "Paul", "Mark",
            "George", "Steven", "Kenneth", "Andrew", "Edward", "Brian", "Joshua", "Kevin" };

    private static final String[] FEMALE_FIRST_NAMES = { "Mary", "Patricia", "Elizabeth", "Jennifer", "Linda", "Barbara",
            "Susan", "Margaret", "Jessica", "Dorothy", "Sarah", "Karen", "Nancy", "Betty", "Lisa", "Sandra", "Helen",
            "Donna", "Ashley", "Kimberly", "Carol", "Michelle", "Amanda", "Emily", "Melissa" };

    private static final String[] FAMILY_NAMES = { "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis",
            "García", "Rodríguez", "Wilson", "Martínez", "Anderson", "Taylor", "Thomas", "Hernández", "Moore", "Martin",
            "Jackson", "Thompson", "White", "López", "Lee", "González", "Harris", "Clark", "Lewis", "Robinson", "Walker",
            "Pérez", "Hall", "Young", "Allen", "Sánchez", "Wright", "King", "Scott", "Green", "Baker", "Adams", "Nelson",
            "Hill", "Ramírez", "Campbell", "Mitchell", "Roberts", "Carter", "Phillips", "Evans", "Turner", "Torres" };

    List<Object> entitiesCreated = new ArrayList<Object>();

    @Autowired @Qualifier("patientService")
    private PatientService patientService;

    @Autowired @Qualifier("encounterService")
    private EncounterService encounterService;

    @Autowired @Qualifier("obsService")
    private ObsService obsService;

    @Autowired @Qualifier("conceptService")
    private ConceptService conceptService;

    @Autowired @Qualifier("locationService")
    private LocationService locationService;

    @Autowired @Qualifier("providerService")
    private ProviderService providerService;
    
    // do not mention VisitService in this class, so that it can be used against multiple OpenMRS versions

    private TestDataBuilder<?> lastBuilder;

    public TestDataManager() {
    }

    public PatientService getPatientService() {
        return patientService;
    }

    public void setPatientService(PatientService patientService) {
        this.patientService = patientService;
    }

    public EncounterService getEncounterService() {
        return encounterService;
    }

    public void setEncounterService(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    public ObsService getObsService() {
        return obsService;
    }

    public void setObsService(ObsService obsService) {
        this.obsService = obsService;
    }

    public ConceptService getConceptService() {
        return conceptService;
    }

    public void setConceptService(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    public ProviderService getProviderService() {
        return providerService;
    }

    public void setProviderService(ProviderService providerService) {
        this.providerService = providerService;
    }

    public LocationService getLocationService() {
        return locationService;
    }

    public void setLocationService(LocationService locationService) {
        this.locationService = locationService;
    }

    private void returningNewBuilder(TestDataBuilder<?> builder) {
        if (lastBuilder != null) {
            if (!lastBuilder.isComplete()) {
                TestDataBuilder didNotSave = lastBuilder;
                lastBuilder = null;
                throw new IllegalStateException("Tried to create a new builder without calling save() on the previous one: " + didNotSave);
            }
        }
        lastBuilder = builder;
    }

    public PatientBuilder patient() {
        PatientBuilder builder = new PatientBuilder(this);
        returningNewBuilder(builder);
        return builder;
    }

    public PatientBuilder randomPatient() {
        String gender = randomElement(GENDERS);
        return patient()
                .age(randomInt(1, 90))
                .gender(gender)
                .name(randomElement(gender.equals("M") ? MALE_FIRST_NAMES : FEMALE_FIRST_NAMES), randomElement(FAMILY_NAMES))
                .identifier(findPatientIdentifierTypeWithNoCheckDigit(), generateRandomIdentifier(), pickRandomLocation());
    }

    public VisitBuilder visit() {
        VisitBuilder builder = new VisitBuilder(this);
        returningNewBuilder(builder);
        return builder;
    }

    public EncounterBuilder encounter() {
        EncounterBuilder builder = new EncounterBuilder(this);
        returningNewBuilder(builder);
        return builder;
    }

    public ProviderBuilder provider() {
        ProviderBuilder builder = new ProviderBuilder(this);
        returningNewBuilder(builder);
        return builder;
    }

    public ProviderBuilder randomProvider() {
        return provider()
                .name(randomElement(FAMILY_NAMES))
                .identifier(generateRandomIdentifier());
    }

    /**
     * You need to actually attach this to a patient or visit before saving this. Prior to OpenMRS 1.9 you also need to set a provider.
     * @return EncounterBuilder with a random EncounterType, Location, and DateTime set
     */
    public EncounterBuilder randomEncounter() {
        return encounter().encounterDatetime(randomRecentDate()).location(pickRandomLocation()).encounterType(pickRandomEncounterType());
    }

    public ObsBuilder obs() {
        ObsBuilder builder = new ObsBuilder(this);
        returningNewBuilder(builder);
        return builder;
    }

    public <T, U extends T> void created(Class<T> clazz, U created) {
        // TODO store these in a more useful way, e.g. by clazz
        entitiesCreated.add(created);
    }

    public int numEntitiesCreated() {
        return entitiesCreated.size();
    }

    private PatientIdentifierType findPatientIdentifierTypeWithNoCheckDigit() {
        for (PatientIdentifierType type : patientService.getAllPatientIdentifierTypes()) {
            if (!type.hasValidator() && StringUtils.isEmpty(type.getFormat())) {
                return type;
            }
        }
        throw new IllegalStateException("Cannot find a PatientIdentifierType with no check digit and no regex");
    }

    private String generateRandomIdentifier() {
        return RandomStringUtils.random(20, true, true);
    }

    private Location pickRandomLocation() {
        return randomElement(locationService.getAllLocations());
    }

    private EncounterType pickRandomEncounterType() {
        return randomElement(encounterService.getAllEncounterTypes());
    }

    private <T> T randomElement(List<T> candidates) {
        return candidates.get(randomInt(0, candidates.size() - 1));
    }

    private <T> T randomElement(T[] candidates) {
        return candidates[randomInt(0, candidates.length - 1)];
    }

    private Date randomRecentDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, randomInt(0, 3));
        cal.add(Calendar.MONTH, randomInt(0, 12));
        cal.add(Calendar.DATE, randomInt(0, 30));
        cal.add(Calendar.HOUR, randomInt(0, 23));
        cal.add(Calendar.MINUTE, randomInt(0, 59));
        cal.add(Calendar.SECOND, randomInt(0, 59));
        return cal.getTime();
    }

}
