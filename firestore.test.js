const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read investigations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("investigations").get());
});

test("Authenticated user: cannot read another user's investigation", async () => {
  // Seed data as admin
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("investigations").doc("inv_1").set({
      userId: BOB_UID,
      title: "Bob Grid Project",
      technologyType: "ADMS",
      vendor: "Vendor B",
      status: "INVESTIGATION",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("investigations").doc("inv_1").get());
});

test("Authenticated user: can create their own valid investigation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_1").set({
      userId: ALICE_UID,
      title: "Smart Transformer Monitoring",
      technologyType: "Grid Edge Sensors",
      vendor: "GridTech Inc",
      status: "INVESTIGATION",
      capexEstimate: 500000,
      opexEstimate: 25000,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: fails creating investigation with invalid status enum", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_2").set({
      userId: ALICE_UID,
      title: "Bad Status Project",
      technologyType: "ADMS",
      vendor: "Vendor X",
      status: "INVALID_STATUS_NAME",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot forge userId to Bob on create", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_3").set({
      userId: BOB_UID,
      title: "Forged User Project",
      technologyType: "ADMS",
      vendor: "Vendor X",
      status: "INVESTIGATION",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create and read stakeholder interview under own investigation", async () => {
  // Seed investigation first
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("investigations").doc("inv_1").set({
      userId: ALICE_UID,
      title: "Substation Automation",
      technologyType: "IEC 61850",
      vendor: "SEL",
      status: "INVESTIGATION",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_1")
      .collection("interviews").doc("interview_1").set({
        userId: ALICE_UID,
        investigationId: "inv_1",
        stakeholderName: "Jane Doe",
        stakeholderRole: "Lead Grid Operations Engineer",
        department: "Substation Engineering",
        priorityRating: 5,
        notes: "Crucial to interface with Maximo work orders for asset health updates.",
        createdAt: new Date(),
        updatedAt: new Date(),
      })
  );
});

test("Authenticated user: can create valid UseCase with feasibility, ROI, and domain", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_1")
      .collection("use_cases").doc("usecase_1").set({
        userId: ALICE_UID,
        investigationId: "inv_1",
        title: "Feeder Overvoltage Curtailment",
        description: "Automated DER reactive power support to prevent tripping.",
        targetSystems: "IBM Maximo, Azure IoT, Snowflake",
        projectedAnnualSavings: 380000,
        feasibilityScore: 89.5,
        projectedRoi: 42.0,
        gridDomain: "DISTRIBUTION",
        riskLevel: "LOW",
        stakeholderBacking: "Approved by Operations and Substation Teams",
        implementationWeeks: 14,
        createdAt: new Date(),
        updatedAt: new Date(),
      })
  );
});

test("Authenticated user: can create and read PilotKpi under own investigation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_1")
      .collection("pilot_kpis").doc("kpi_1").set({
        userId: ALICE_UID,
        investigationId: "inv_1",
        metricName: "Feeder Inverter Inrush Efficiency Gain",
        category: "EFFICIENCY",
        currentValue: 14.2,
        targetValue: 12.0,
        unit: "%",
        notes: "Demonstrated 14.2% reduction in reactive power loss across 3 capacitor banks.",
        createdAt: new Date(),
        updatedAt: new Date(),
      })
  );

  const snap = await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_1")
      .collection("pilot_kpis").doc("kpi_1").get()
  );
});

test("Authenticated user: cannot read another user's PilotKpi", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_1")
      .collection("pilot_kpis").doc("kpi_1").get()
  );
});

test("Authenticated user: fails creating PilotKpi with invalid category", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("investigations").doc("inv_1")
      .collection("pilot_kpis").doc("kpi_2").set({
        userId: ALICE_UID,
        investigationId: "inv_1",
        metricName: "Bad Category Metric",
        category: "INVALID_KPI_CATEGORY",
        currentValue: 99.0,
        targetValue: 100.0,
        unit: "%",
        createdAt: new Date(),
        updatedAt: new Date(),
      })
  );
});

test("Authenticated user: can create, update, and read StakeholderProfile under own user path", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdAt = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stakeholder_profiles").doc("profile_1").set({
      userId: ALICE_UID,
      name: "Victoria Sterling",
      role: "SVP of Grid Modernization",
      department: "Executive Operations",
      email: "v.sterling@gridutility.com",
      phone: "+1 (555) 214-8900",
      officeLocation: "Corporate HQ - Floor 14",
      preferredChannel: "Executive Briefing",
      gridDomain: "DISTRIBUTION",
      influenceLevel: "EXECUTIVE_SPONSOR",
      influenceScore: 96,
      stance: "POSITIVE",
      projectResponsibilities: "Chairs Capital Investment Committee",
      notes: "Focused on SAIDI reduction.",
      createdAt,
      updatedAt: createdAt,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stakeholder_profiles").doc("profile_1").update({
      influenceScore: 98,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stakeholder_profiles").doc("profile_1").get()
  );
});

test("Authenticated user: cannot read another user's StakeholderProfile (PII isolation)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("stakeholder_profiles").doc("profile_1").set({
      userId: ALICE_UID,
      name: "Victoria Sterling",
      role: "SVP of Grid Modernization",
      department: "Executive Operations",
      email: "v.sterling@gridutility.com",
      phone: "+1 (555) 214-8900",
      gridDomain: "DISTRIBUTION",
      influenceLevel: "EXECUTIVE_SPONSOR",
      influenceScore: 96,
      stance: "POSITIVE",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("stakeholder_profiles").doc("profile_1").get()
  );
});

test("Authenticated user: fails creating StakeholderProfile with invalid influenceLevel or out-of-range score", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("stakeholder_profiles").doc("profile_bad").set({
      userId: ALICE_UID,
      name: "Invalid Influencer",
      role: "Engineer",
      department: "Ops",
      gridDomain: "DISTRIBUTION",
      influenceLevel: "NOT_A_VALID_LEVEL",
      influenceScore: 150,
      stance: "POSITIVE",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can create, update, and read StakeholderInteraction under own user path", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdAt = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stakeholder_interactions").doc("inter_1").set({
      userId: ALICE_UID,
      investigationId: "inv_1",
      stakeholderName: "Dave Kowalski",
      stakeholderRole: "Transmission Line Superintendent",
      department: "Field Maintenance",
      gridDomain: "TRANSMISSION",
      communicationChannel: "Field Crew Safety Stand-Down",
      interactionDateLabel: "2025-02-20",
      communicationSummary: "Discussed automated line sensor triage and Maximo dispatch.",
      identifiedRebuttals: "Crews feared headcount reduction and extra tablet complexity.",
      rebuttalCategory: "WORKFORCE_AUGMENTATION",
      buyInStrategy: "Position sensors as substituting tedious manual foot patrols in hazardous terrain with zero job loss.",
      buyInStatus: "STRATEGY_IN_PROGRESS",
      laborEfficiencyGainPct: 32.0,
      safetyImprovementPct: 41.0,
      workflowOptimizationPct: 38.0,
      createdAt,
      updatedAt: createdAt,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stakeholder_interactions").doc("inter_1").update({
      buyInStatus: "BUY_IN_ACHIEVED",
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("stakeholder_interactions").doc("inter_1").get()
  );
});

test("Authenticated user: cannot read another user's StakeholderInteraction", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("stakeholder_interactions").doc("inter_1").set({
      userId: ALICE_UID,
      investigationId: "inv_1",
      stakeholderName: "Dave Kowalski",
      stakeholderRole: "Transmission Line Superintendent",
      department: "Field Maintenance",
      gridDomain: "TRANSMISSION",
      communicationChannel: "Field Crew Safety Stand-Down",
      interactionDateLabel: "2025-02-20",
      communicationSummary: "Discussed automated line sensor triage.",
      identifiedRebuttals: "Crews feared headcount reduction.",
      rebuttalCategory: "WORKFORCE_AUGMENTATION",
      buyInStrategy: "Substitute tedious manual patrols.",
      buyInStatus: "BUY_IN_ACHIEVED",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("stakeholder_interactions").doc("inter_1").get()
  );
});

test("Authenticated user: fails creating StakeholderInteraction with invalid buyInStatus", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("stakeholder_interactions").doc("inter_bad").set({
      userId: ALICE_UID,
      investigationId: "inv_1",
      stakeholderName: "Dave Kowalski",
      stakeholderRole: "Superintendent",
      gridDomain: "TRANSMISSION",
      communicationChannel: "Sync",
      interactionDateLabel: "2025-02-20",
      communicationSummary: "Summary",
      identifiedRebuttals: "Rebuttal",
      rebuttalCategory: "WORKFORCE_AUGMENTATION",
      buyInStrategy: "Strategy",
      buyInStatus: "INVALID_BUY_IN_STATUS",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
