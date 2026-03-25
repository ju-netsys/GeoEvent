const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  HeadingLevel, AlignmentType, BorderStyle, WidthType, ShadingType,
  LevelFormat, PageNumber, Header, Footer, PageBreak
} = require('C:/Users/juroq/AppData/Roaming/npm/node_modules/docx');
const fs = require('fs');

const VIOLET = "7C57C9";
const VIOLET_LIGHT = "EDE8F9";
const GREY_BG = "F4F4F4";
const WHITE = "FFFFFF";
const DARK = "1A1A2E";
const BLUE = "1565C0";
const GREEN = "2E7D32";

function h1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 400, after: 200 },
    children: [new TextRun({ text, bold: true, size: 36, color: VIOLET, font: "Arial" })]
  });
}
function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 300, after: 140 },
    children: [new TextRun({ text, bold: true, size: 28, color: DARK, font: "Arial" })]
  });
}
function h3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 220, after: 100 },
    children: [new TextRun({ text, bold: true, size: 24, color: VIOLET, font: "Arial" })]
  });
}
function p(text, { bold = false, italic = false, color = DARK } = {}) {
  return new Paragraph({
    spacing: { before: 60, after: 60 },
    children: [new TextRun({ text, bold, italic, color, size: 22, font: "Arial" })]
  });
}
function code(text) {
  return new Paragraph({
    spacing: { before: 80, after: 80 },
    indent: { left: 360 },
    children: [new TextRun({ text, font: "Courier New", size: 18, color: "444444" })]
  });
}
function bullet(text, { bold = false } = {}) {
  return new Paragraph({
    numbering: { reference: "bullets", level: 0 },
    spacing: { before: 40, after: 40 },
    children: [new TextRun({ text, bold, size: 22, font: "Arial", color: DARK })]
  });
}
function pageBreak() {
  return new Paragraph({ children: [new PageBreak()] });
}

function fileTable(rows) {
  const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
  const borders = { top: border, bottom: border, left: border, right: border };
  const headerRow = new TableRow({
    tableHeader: true,
    children: ["Fichier", "Type OO", "Responsabilite principale"].map((h, i) =>
      new TableCell({
        borders, shading: { fill: VIOLET, type: ShadingType.CLEAR },
        margins: { top: 80, bottom: 80, left: 120, right: 120 },
        width: { size: [2800, 1600, 4960][i], type: WidthType.DXA },
        children: [new Paragraph({ children: [new TextRun({ text: h, bold: true, color: WHITE, font: "Arial", size: 20 })] })]
      })
    )
  });
  const dataRows = rows.map(([file, type, role]) =>
    new TableRow({
      children: [
        new TableCell({ borders, margins: { top: 80, bottom: 80, left: 120, right: 120 }, width: { size: 2800, type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun({ text: file, font: "Courier New", size: 18, color: VIOLET })] })]
        }),
        new TableCell({ borders, shading: { fill: GREY_BG, type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 }, width: { size: 1600, type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun({ text: type, size: 20, font: "Arial", color: DARK })] })]
        }),
        new TableCell({ borders, margins: { top: 80, bottom: 80, left: 120, right: 120 }, width: { size: 4960, type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun({ text: role, size: 20, font: "Arial", color: DARK })] })]
        }),
      ]
    })
  );
  return new Table({ width: { size: 9360, type: WidthType.DXA }, columnWidths: [2800, 1600, 4960], rows: [headerRow, ...dataRows] });
}

function conceptTable(rows) {
  const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
  const borders = { top: border, bottom: border, left: border, right: border };
  const headerRow = new TableRow({
    tableHeader: true,
    children: ["Concept", "Definition & application dans le projet"].map((h, i) =>
      new TableCell({
        borders, shading: { fill: VIOLET, type: ShadingType.CLEAR },
        margins: { top: 80, bottom: 80, left: 120, right: 120 },
        width: { size: i === 0 ? 2500 : 6860, type: WidthType.DXA },
        children: [new Paragraph({ children: [new TextRun({ text: h, bold: true, color: WHITE, font: "Arial", size: 20 })] })]
      })
    )
  });
  const dataRows = rows.map(([concept, def]) =>
    new TableRow({
      children: [
        new TableCell({ borders, shading: { fill: VIOLET_LIGHT, type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 }, width: { size: 2500, type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun({ text: concept, bold: true, size: 20, font: "Arial", color: VIOLET })] })]
        }),
        new TableCell({ borders, margins: { top: 80, bottom: 80, left: 120, right: 120 }, width: { size: 6860, type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun({ text: def, size: 20, font: "Arial", color: DARK })] })]
        }),
      ]
    })
  );
  return new Table({ width: { size: 9360, type: WidthType.DXA }, columnWidths: [2500, 6860], rows: [headerRow, ...dataRows] });
}

function techChoiceTable(rows) {
  const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
  const borders = { top: border, bottom: border, left: border, right: border };
  const headerRow = new TableRow({
    tableHeader: true,
    children: ["Critere", "Firebase Firestore", "SQLite / Room", "Realm"].map((h, i) =>
      new TableCell({
        borders, shading: { fill: VIOLET, type: ShadingType.CLEAR },
        margins: { top: 80, bottom: 80, left: 120, right: 120 },
        width: { size: [2000, 2453, 2453, 2454][i], type: WidthType.DXA },
        children: [new Paragraph({ children: [new TextRun({ text: h, bold: true, color: WHITE, font: "Arial", size: 18 })] })]
      })
    )
  });
  const dataRows = rows.map((cells) =>
    new TableRow({
      children: cells.map((text, i) =>
        new TableCell({
          borders,
          shading: { fill: i === 0 ? GREY_BG : (i === 1 ? VIOLET_LIGHT : WHITE), type: ShadingType.CLEAR },
          margins: { top: 80, bottom: 80, left: 120, right: 120 },
          width: { size: [2000, 2453, 2453, 2454][i], type: WidthType.DXA },
          children: [new Paragraph({ children: [new TextRun({ text, bold: i === 0, size: 18, font: "Arial", color: i === 0 ? DARK : (i === 1 ? VIOLET : "888888") })] })]
        })
      )
    })
  );
  return new Table({ width: { size: 9360, type: WidthType.DXA }, columnWidths: [2000, 2453, 2453, 2454], rows: [headerRow, ...dataRows] });
}

const doc = new Document({
  numbering: {
    config: [{
      reference: "bullets",
      levels: [{ level: 0, format: LevelFormat.BULLET, text: "\u2022", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } } }]
    }]
  },
  styles: {
    default: { document: { run: { font: "Arial", size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 36, bold: true, font: "Arial", color: VIOLET },
        paragraph: { spacing: { before: 400, after: 200 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 28, bold: true, font: "Arial", color: DARK },
        paragraph: { spacing: { before: 300, after: 140 }, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 24, bold: true, font: "Arial", color: VIOLET },
        paragraph: { spacing: { before: 220, after: 100 }, outlineLevel: 2 } },
    ]
  },
  sections: [{
    properties: {
      page: { size: { width: 12240, height: 15840 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } }
    },
    headers: {
      default: new Header({ children: [new Paragraph({
        border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: VIOLET } },
        children: [new TextRun({ text: "GeoEvent Kotlin  \u2014  Documentation technique", font: "Arial", size: 18, color: "888888" })]
      })] })
    },
    footers: {
      default: new Footer({ children: [new Paragraph({
        alignment: AlignmentType.RIGHT,
        children: [
          new TextRun({ text: "Page ", font: "Arial", size: 18, color: "888888" }),
          new TextRun({ children: [PageNumber.CURRENT], font: "Arial", size: 18, color: "888888" }),
          new TextRun({ text: " / ", font: "Arial", size: 18, color: "888888" }),
          new TextRun({ children: [PageNumber.TOTAL_PAGES], font: "Arial", size: 18, color: "888888" }),
        ]
      })] })
    },
    children: [

      // PAGE DE GARDE
      new Paragraph({ spacing: { before: 1440, after: 200 }, alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: "\uD83E\uDD9B", size: 120 })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 0, after: 120 },
        children: [new TextRun({ text: "GeoEvent Kotlin", bold: true, size: 64, font: "Arial", color: VIOLET })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 0, after: 200 },
        children: [new TextRun({ text: "Documentation technique de l\u2019architecture", size: 28, font: "Arial", color: "666666" })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 0, after: 120 },
        children: [new TextRun({ text: "Repartition P1 \u00B7 P2 \u00B7 P3", bold: true, size: 32, font: "Arial", color: DARK })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 0, after: 600 },
        children: [new TextRun({ text: "Mars 2026  \u2014  ITII", size: 22, font: "Arial", color: "888888" })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 0, after: 80 },
        children: [new TextRun({ text: "Fonctionnalites implementees :", bold: true, size: 22, font: "Arial", color: DARK })] }),
      ...[
        "Navigation bas de page : onglet Carte / onglet Liste des evenements",
        "Description et auteur visibles dans la bulle de chaque marqueur",
        "Modification d\u2019un evenement via bouton crayon (proprietaire uniquement)",
        "Politique de mot de passe avec indicateurs visuels rouge / vert en temps reel",
        "Affichage de l\u2019auteur (email) sur chaque evenement de la carte",
        "Recherche de lieu par code postal (zippopotam.us)",
        "Suppression en temps reel via listener Firestore",
      ].map(f => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 40, after: 0 },
        children: [new TextRun({ text: "\u2022 " + f, size: 20, font: "Arial", color: "444444" })] })),
      pageBreak(),

      // 1. VUE D'ENSEMBLE
      h1("1. Vue d\u2019ensemble de l\u2019architecture"),
      p("GeoEvent Kotlin est une application Android native developpee en Kotlin. Les utilisateurs authentifies placent des evenements geolocalises sur une carte OpenStreetMap, les consultent depuis un onglet liste, et peuvent les modifier ou les supprimer."),
      p("L\u2019architecture suit le patron MVVM (Model\u2013View\u2013ViewModel) couple au Repository Pattern, recommande par Google pour les applications Android modernes."),
      new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),
      conceptTable([
        ["MVVM", "Separation stricte entre la couche UI (View), la logique metier (ViewModel) et les donnees (Model). La View ne parle jamais directement a la base de donnees."],
        ["Repository Pattern", "Interface unique pour acceder aux donnees. Le ViewModel ne sait pas si les donnees viennent de Firestore, d\u2019un cache ou d\u2019une API REST. Il appelle seulement getEvents() / addEvent()."],
        ["Dependency Injection", "Les dependances (FirestoreDataSource, EventRepository) sont instanciees une seule fois dans ServiceLocator et injectees la ou c\u2019est necessaire."],
        ["Single Responsibility", "Chaque classe a une seule raison de changer. FirestoreDataSource gere uniquement la connexion Firebase. FirestoreEventRepository traduit les appels abstraits en appels concrets."],
        ["Open/Closed", "MapService est une interface. Ajouter un moteur de carte (Google Maps, Mapbox) ne necessite pas de modifier MainActivity."],
        ["Kotlin Flow / Coroutines", "Les donnees Firestore sont exposees comme Flow<List<Event>>. L\u2019UI collecte ce flux et se met a jour automatiquement a chaque changement en base."],
      ]),
      new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),
      h2("Structure des packages"),
      code("fr.itii.geoevent_kotlin"),
      code("\u251C\u2500\u2500 p1/     \u2192 Couche donnees & DI (Event, Repository, DataSource, ServiceLocator)"),
      code("\u251C\u2500\u2500 p2/     \u2192 Authentification (AuthViewModel, LoginActivity, RegisterActivity)"),
      code("\u251C\u2500\u2500 p3/     \u2192 Carte & Evenements (MapService, OsmMapService, MainActivity,"),
      code("\u2502              EventAdapter, MapViewModel, EventViewModel,"),
      code("\u2502              AddEventActivity, EventDetailActivity)"),
      code("\u2514\u2500\u2500 common/ \u2192 Utilitaires partages (UiState, ViewModelFactory)"),
      pageBreak(),

      // CHOIX TECHNOLOGIQUES
      h1("2. Choix technologiques"),
      p("Cette section justifie le choix de Firebase Firestore, osmdroid et Firebase Authentication par rapport aux alternatives disponibles."),

      h2("2.1  Base de donnees : Firebase Firestore"),
      p("GeoEvent est une application collaborative : plusieurs utilisateurs deposent des evenements visibles par tous en temps reel. Ce besoin oriente naturellement vers un backend cloud."),
      new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),
      techChoiceTable([
        ["Temps reel", "Listener natif. L\u2019UI se met a jour sans polling.", "Polling manuel ou LiveData local", "Pas de sync cloud native"],
        ["Hebergement", "Cloud Google, zero configuration serveur", "Local uniquement (hors API custom)", "Local ou Realm Atlas (payant)"],
        ["Authentification", "Integration directe avec Firebase Auth", "Independant", "Independant"],
        ["Hors ligne", "Cache automatique, sync a la reconnexion", "Natif (local)", "Natif (local)"],
        ["Complexite setup", "Faible : google-services.json + 2 dependances", "Moderee : schema SQL, migrations", "Faible : SDK autonome"],
        ["Scalabilite", "Automatique, gere par Google", "Limitee au stockage local", "Limitee sans Realm Atlas"],
      ]),
      new Paragraph({ spacing: { before: 200, after: 80 }, children: [] }),
      p("Conclusion : Firestore est retenu car il offre la synchronisation temps reel multi-utilisateurs sans necessiter de backend personnalise. Le listener addSnapshotListener maintient la carte a jour automatiquement pour tous les appareils connectes.", { bold: true }),

      h2("2.2  Cartographie : osmdroid (OpenStreetMap)"),
      p("La carte est le coeur de l\u2019application. Deux bibliotheques majeures etaient candidates : le SDK Google Maps et osmdroid."),
      new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),
      new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [2000, 3680, 3680],
        rows: [
          new TableRow({ tableHeader: true, children: ["Critere", "osmdroid (OpenStreetMap)", "Google Maps SDK"].map((h, i) =>
            new TableCell({ borders: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" }, bottom: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" }, left: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" }, right: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" } },
              shading: { fill: VIOLET, type: ShadingType.CLEAR }, margins: { top: 80, bottom: 80, left: 120, right: 120 },
              width: { size: [2000, 3680, 3680][i], type: WidthType.DXA },
              children: [new Paragraph({ children: [new TextRun({ text: h, bold: true, color: WHITE, font: "Arial", size: 18 })] })]
            })
          )}),
          ...([
            ["Licence", "Open Source (Apache 2.0), aucun quota", "Cle API requise, quotas d\u2019appels (payant au-dela)"],
            ["Cle API", "Aucune - agent HTTP suffit", "Obligatoire + facturation possible"],
            ["Donnees", "OpenStreetMap, contributeurs mondiaux", "Google Maps, mise a jour proprietaire"],
            ["Personnalisation", "Complete : tuiles, marqueurs, overlays libres", "Limitee par les CGU Google"],
            ["Dependance", "Zero dependance a un service tiers payant", "Dependance totale a l\u2019infrastructure Google"],
          ]).map(cells => new TableRow({ children: cells.map((text, i) =>
            new TableCell({ borders: { top: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" }, bottom: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" }, left: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" }, right: { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" } },
              shading: { fill: i === 0 ? GREY_BG : (i === 1 ? VIOLET_LIGHT : WHITE), type: ShadingType.CLEAR },
              margins: { top: 80, bottom: 80, left: 120, right: 120 },
              width: { size: [2000, 3680, 3680][i], type: WidthType.DXA },
              children: [new Paragraph({ children: [new TextRun({ text, bold: i === 0, size: 18, font: "Arial", color: i === 0 ? DARK : (i === 1 ? VIOLET : "888888") })] })]
            })
          )}))
        ]
      }),
      new Paragraph({ spacing: { before: 200, after: 80 }, children: [] }),
      p("Conclusion : osmdroid est retenu car il est totalement gratuit, open source, et sans dependance a une cle API. L\u2019implementation est abstraite derriere l\u2019interface MapService, ce qui permettrait de basculer vers Google Maps sans modifier l\u2019UI.", { bold: true }),

      h2("2.3  Authentification : Firebase Authentication"),
      p("L\u2019application necessite une identification des utilisateurs pour controler la propriete des evenements (modification / suppression)."),
      conceptTable([
        ["Firebase Auth", "Solution cle en main : email/password, OAuth, SMS. Integration native avec Firestore (Rules basees sur request.auth.uid). Aucun backend a maintenir."],
        ["Custom JWT backend", "Necessite un serveur Node/Python, une base d\u2019utilisateurs, la gestion des tokens. Hors scope pour un projet pedagogique."],
        ["Auth0 / Supabase", "Solutions tierces valables mais ajoutent une dependance supplementaire et une courbe d\u2019apprentissage inutile si Firebase est deja utilise."],
        ["Pas d\u2019auth", "Impossible : sans identification, n\u2019importe qui pourrait supprimer les evenements des autres. Les Firestore Security Rules exigent request.auth != null."],
      ]),
      p("Conclusion : Firebase Authentication s\u2019integre nativement avec Firestore, partage la meme SDK, et necessite zero infrastructure serveur. C\u2019est le choix le plus coherent dans l\u2019ecosysteme Firebase.", { bold: true }),
      pageBreak(),

      // P1
      new Paragraph({ spacing: { before: 0, after: 200 }, children: [
        new TextRun({ text: "P1", bold: true, size: 80, font: "Arial", color: WHITE, shading: { type: ShadingType.CLEAR, fill: VIOLET } }),
        new TextRun({ text: "  Couche donnees & Architecture", bold: true, size: 44, font: "Arial", color: VIOLET })
      ]}),
      p("P1 est responsable de tout ce qui touche a la persistance, a l\u2019abstraction des donnees et a l\u2019injection de dependances. C\u2019est la fondation sur laquelle P2 et P3 s\u2019appuient."),
      h2("3.1  Fichiers assignes a P1"),
      p("Chemin : app/src/main/java/fr/itii/geoevent_kotlin/p1/", { italic: true }),
      fileTable([
        ["Event.kt", "data class", "Modele de domaine : id, title, description, latitude, longitude, userId, authorEmail, createdAt"],
        ["EventRepository.kt", "interface", "Contrat abstrait : getEvents, addEvent, updateEvent, deleteEvent"],
        ["FirestoreEventRepository.kt", "class (impl.)", "Delegation vers FirestoreDataSource - traduit les appels abstraits en appels concrets"],
        ["FirestoreDataSource.kt", "class", "Seule classe a importer Firebase Firestore - listener temps reel, CRUD, gestion PERMISSION_DENIED"],
        ["ServiceLocator.kt", "object (singleton)", "Conteneur de dependances - instancie DataSource et Repository une seule fois (by lazy)"],
      ]),
      h2("3.2  Analyse detaillee"),
      h3("p1/Event.kt  \u2014  data class"),
      p("Entite centrale de l\u2019application. Annotee @Parcelize pour etre transmise entre Activity via Intent. Tous les champs ont une valeur par defaut pour que Firestore puisse deserialiser les documents existants."),
      conceptTable([
        ["@Parcelize", "Genere automatiquement writeToParcel() / createFromParcel(). Evite le code boilerplate de serialisation."],
        ["@DocumentId", "Annotation Firestore : l\u2019identifiant du document est injecte automatiquement dans le champ id lors de la deserialisation."],
        ["@RawValue", "Le Timestamp Firebase n\u2019est pas Parcelable. @RawValue indique a @Parcelize de l\u2019ignorer dans la verification de type."],
        ["authorEmail", "Champ ajoute pour afficher l\u2019auteur dans la bulle marqueur. Rempli au moment de la creation depuis FirebaseAuth.currentUser.email."],
      ]),
      code("data class Event("),
      code("    @DocumentId val id: String = \"\","),
      code("    val title: String = \"\","),
      code("    val description: String = \"\","),
      code("    val latitude: Double = 0.0,"),
      code("    val longitude: Double = 0.0,"),
      code("    val userId: String = \"\","),
      code("    val authorEmail: String = \"\","),
      code("    val createdAt: @RawValue Timestamp = Timestamp.now()"),
      code(") : Parcelable"),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h3("p1/EventRepository.kt  \u2014  interface"),
      p("Contrat que doit respecter toute source de donnees. Application du principe Dependency Inversion : les ViewModels dependent de l\u2019abstraction, jamais de l\u2019implementation Firestore."),
      code("interface EventRepository {"),
      code("    fun getEvents(): Flow<List<Event>>"),
      code("    suspend fun addEvent(event: Event): Result<Unit>"),
      code("    suspend fun updateEvent(event: Event): Result<Unit>"),
      code("    suspend fun deleteEvent(eventId: String): Result<Unit>"),
      code("}"),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h3("p1/FirestoreDataSource.kt  \u2014  acces Firebase"),
      p("Seule classe a importer com.google.firebase.firestore.*. Tous les appels Firebase sont isoles ici."),
      conceptTable([
        ["callbackFlow { }", "Convertit le callback addSnapshotListener en Flow Kotlin reactif. L\u2019UI recoit les mises a jour en temps reel sans polling."],
        ["awaitClose { }", "Supprime le listener Firestore quand le Flow est annule - evite les fuites memoire."],
        ["PERMISSION_DENIED", "A la deconnexion, Firestore envoie cette erreur. Le Flow se ferme proprement via close() sans argument plutot que close(error) pour ne pas crasher l\u2019UI."],
        ["updateEvent()", "Utilise .set(event) pour ecraser le document entier avec les nouvelles valeurs. Preserve userId et authorEmail depuis l\u2019objet Event.copy()."],
      ]),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h3("p1/ServiceLocator.kt  \u2014  singleton DI"),
      p("Objet Kotlin (singleton) qui instancie les dependances en lazy. Toutes les Activity partagent la meme instance, donc un seul listener Firestore actif a la fois."),
      code("object ServiceLocator {"),
      code("    private val firestoreDataSource by lazy { FirestoreDataSource() }"),
      code("    val eventRepository: EventRepository by lazy {"),
      code("        FirestoreEventRepository(firestoreDataSource)"),
      code("    }"),
      code("}"),
      h2("3.3  Ce que P1 doit maitriser"),
      bullet("Kotlin : data class, object, sealed class, by lazy, suspend fun, Flow<T>"),
      bullet("Coroutines : callbackFlow, awaitClose, trySend, launch"),
      bullet("Firebase Firestore : collections, documents, addSnapshotListener, .await()"),
      bullet("Principes SOLID : Single Responsibility, Open/Closed, Dependency Inversion"),
      bullet("Repository Pattern : pourquoi separer interface et implementation"),
      bullet("Result<T> : isSuccess, exceptionOrNull() pour la gestion d\u2019erreurs"),
      pageBreak(),

      // P2
      new Paragraph({ spacing: { before: 0, after: 200 }, children: [
        new TextRun({ text: "P2", bold: true, size: 80, font: "Arial", color: WHITE, shading: { type: ShadingType.CLEAR, fill: BLUE } }),
        new TextRun({ text: "  Authentification", bold: true, size: 44, font: "Arial", color: BLUE })
      ]}),
      p("P2 est responsable de tout le systeme d\u2019authentification : inscription avec politique de mot de passe, connexion, deconnexion et gestion des etats de session via Firebase Authentication."),
      h2("4.1  Fichiers assignes a P2"),
      p("Chemin : app/src/main/java/fr/itii/geoevent_kotlin/p2/", { italic: true }),
      fileTable([
        ["AuthViewModel.kt", "ViewModel", "Logique metier auth - appelle FirebaseAuth, expose authState : StateFlow<UiState<Unit>?>"],
        ["LoginActivity.kt", "Activity (View)", "Ecran connexion - email + mot de passe, navigation vers p3.MainActivity"],
        ["RegisterActivity.kt", "Activity (View)", "Inscription - validation politique mdp, indicateurs visuels rouge/vert en temps reel"],
      ]),
      h2("4.2  Analyse detaillee"),
      h3("p2/AuthViewModel.kt  \u2014  ViewModel"),
      p("Seul ViewModel autorise a importer FirebaseAuth directement. Expose authState : StateFlow<UiState<Unit>?>."),
      conceptTable([
        ["MutableStateFlow", "Variable d\u2019etat reactive. Quand sa valeur change, tous les collecteurs sont notifies automatiquement."],
        ["StateFlow", "Version lecture seule de MutableStateFlow. Empeche l\u2019Activity de modifier l\u2019etat directement."],
        ["viewModelScope", "CoroutineScope lie au cycle de vie du ViewModel. Coroutines annulees automatiquement a la destruction."],
        ["try/catch", "Remplace runCatching pour une lisibilite accrue. Le bloc catch appelle UiState.Error avec le message d\u2019exception."],
      ]),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h3("p2/RegisterActivity.kt  \u2014  politique de mot de passe"),
      p("L\u2019ecran d\u2019inscription valide le mot de passe en temps reel via un TextWatcher. Cinq TextViews (tvReqLength, tvReqUpper, tvReqLower, tvReqDigit, tvReqSpecial) changent de couleur a chaque caractere frappe."),
      conceptTable([
        ["TextWatcher", "Interface Android appelee a chaque modification de l\u2019EditText. afterTextChanged() declenche la mise a jour des indicateurs."],
        ["Indicateurs rouge/vert", "Chaque TextView affiche une exigence. La couleur passe de #C62828 (rouge) a #2E7D32 (vert) quand la condition est satisfaite."],
        ["isPasswordValid()", "Verifie les 5 conditions : longueur >= 8, 1 majuscule, 1 minuscule, 1 chiffre, 1 caractere special (any { !it.isLetterOrDigit() })."],
        ["Regles enforced", "8 caracteres minimum, 1 majuscule, 1 minuscule, 1 chiffre, 1 caractere special. Validation client uniquement."],
      ]),
      code("fun isPasswordValid(password: String): Boolean {"),
      code("    if (password.length < 8) return false"),
      code("    if (!password.any { it.isUpperCase() }) return false"),
      code("    if (!password.any { it.isLowerCase() }) return false"),
      code("    if (!password.any { it.isDigit() }) return false"),
      code("    if (!password.any { !it.isLetterOrDigit() }) return false"),
      code("    return true"),
      code("}"),
      h2("4.3  Ce que P2 doit maitriser"),
      bullet("Firebase Authentication : signInWithEmailAndPassword, createUserWithEmailAndPassword, signOut"),
      bullet("Kotlin coroutines : .await() sur les Task Firebase, viewModelScope.launch, try/catch"),
      bullet("StateFlow / MutableStateFlow : exposer l\u2019etat en lecture seule"),
      bullet("TextWatcher : interface, afterTextChanged(), mise a jour UI en temps reel"),
      bullet("Validation mot de passe : any { }, all { }, fonctions d\u2019extension Char"),
      bullet("ViewBinding : ActivityRegisterBinding, acces secure aux vues"),
      bullet("Material 3 : TextInputLayout, TextInputEditText, erreurs inline"),
      pageBreak(),

      // P3
      new Paragraph({ spacing: { before: 0, after: 200 }, children: [
        new TextRun({ text: "P3", bold: true, size: 80, font: "Arial", color: WHITE, shading: { type: ShadingType.CLEAR, fill: GREEN } }),
        new TextRun({ text: "  Carte & Gestion des evenements", bold: true, size: 44, font: "Arial", color: GREEN })
      ]}),
      p("P3 est responsable de la carte interactive, des marqueurs, des popups enrichis, de la navigation par onglets et des ecrans de creation/edition/suppression d\u2019evenements."),
      h2("5.1  Fichiers assignes a P3"),
      p("Chemin : app/src/main/java/fr/itii/geoevent_kotlin/p3/", { italic: true }),
      fileTable([
        ["MapService.kt", "interface", "Abstraction cartographique : showMap, addMarker (avec description, authorEmail), setListeners"],
        ["MapState.kt", "data class", "Dans MapService.kt - persiste centre (lat/lon) et zoom pour onSaveInstanceState"],
        ["OsmMapService.kt", "class (impl.)", "Implémentation osmdroid - Marker, Overlay, MarkerMeta (description, authorEmail, userId)"],
        ["EventAdapter.kt", "RecyclerView Adapter", "Affiche la liste des evenements : titre, description, coordonnees. Clic teleporte sur la carte."],
        ["MainActivity.kt", "Activity (View)", "Carte + liste onglets + barre recherche + FAB + popups marqueur/carte"],
        ["MapViewModel.kt", "ViewModel", "Observe les evenements Firestore, expose eventsState et deleteState"],
        ["EventViewModel.kt", "ViewModel", "addEvent, updateEvent, deleteEvent - partagé AddEventActivity et EventDetailActivity"],
        ["AddEventActivity.kt", "Activity (View)", "Formulaire creation ET edition d\u2019evenement - detecte EXTRA_EVENT pour le mode edition"],
        ["EventDetailActivity.kt", "Activity (View)", "Detail d\u2019un evenement - bouton supprimer visible uniquement pour le proprietaire"],
      ]),
      h2("5.2  Analyse detaillee"),
      h3("p3/OsmMapService.kt  \u2014  MarkerMeta"),
      p("Le champ title du Marker osmdroid est le seul champ de type String. Pour stocker description, authorEmail et userId, une Map<String, MarkerMeta> indexee par eventId est maintenue cote OsmMapService."),
      conceptTable([
        ["MarkerMeta", "data class interne : description, authorEmail, userId. Instanciee a chaque appel addMarker() et effacee dans clearMarkers()."],
        ["mapView.projection.toPixels()", "Convertit la GeoPoint du marqueur en coordonnees ecran (pixels) pour positionner le PopupWindow precisement au-dessus du marqueur."],
        ["Overlay (tap carte)", "Overlay generique traite en dernier. Capte les taps sur la carte vide - les Marker ont priorite via setOnMarkerClickListener."],
      ]),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h3("p3/EventAdapter.kt  \u2014  RecyclerView Adapter"),
      p("Affiche la liste complete des evenements dans l\u2019onglet Liste. Chaque item affiche le titre (gras), la description (jusqu\u2019a 2 lignes) et les coordonnees en police monospace."),
      conceptTable([
        ["submitList()", "Remplace la liste interne et appelle notifyDataSetChanged(). Simple et suffisant pour cette taille de collection."],
        ["ViewHolder pattern", "Evite de re-inflater le layout a chaque scroll. La vue est creee une fois et recyclee via onBindViewHolder."],
        ["Clic item", "Le lambda onEventClick rappelle MainActivity qui selectionne l\u2019onglet Carte et centre la carte sur l\u2019evenement : mapService.centerOn(lat, lon, 15.0)."],
      ]),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h3("p3/MainActivity.kt  \u2014  navigation par onglets"),
      p("La BottomNavigationView gere deux onglets : Carte (mapContainer + searchCard + FAB) et Liste (rvEvents). Le passage a l\u2019onglet Carte depuis la liste ferme les popups ouverts."),
      conceptTable([
        ["BottomNavigationView", "Composant Material 3. setOnItemSelectedListener retourne true pour confirmer la selection. Visibilite des vues changee en consequence."],
        ["currentEvents", "Liste List<Event> maintenue a jour dans displayEvents(). Utilisee dans showMarkerPopup() pour retrouver l\u2019Event complet par id et lancer AddEventActivity en mode edition."],
        ["Bulle marqueur enrichie", "popup_marker_click.xml affiche : titre, description (GONE si vide), auteur (GONE si vide), boutons edition/suppression (GONE si non proprietaire)."],
        ["Mode edition", "Bouton crayon visible uniquement si currentUser.uid == userId. Lance AddEventActivity avec EXTRA_EVENT = event (objet Parcelable complet)."],
      ]),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h3("p3/AddEventActivity.kt  \u2014  creation et edition"),
      p("Si l\u2019Intent contient EXTRA_EVENT, l\u2019Activity est en mode edition : les champs sont pre-remplis et le bouton appelle viewModel.updateEvent(). Sinon, c\u2019est une creation."),
      code("// Mode detection"),
      code("eventToEdit = intent.getParcelableExtra(EXTRA_EVENT)"),
      code("if (eventToEdit != null) {"),
      code("    prefillForm(eventToEdit!!)"),
      code("    viewModel.updateEvent(eventToEdit!!, title, description, lat, lon)"),
      code("} else {"),
      code("    viewModel.addEvent(title, description, lat, lon)"),
      code("}"),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h3("p3/EventViewModel.kt  \u2014  updateEvent"),
      p("updateEvent() utilise Event.copy() pour produire un nouvel objet avec les champs modifies tout en preservant userId, authorEmail et createdAt de l\u2019original."),
      code("fun updateEvent(original: Event, title: String, description: String,"),
      code("                latitude: Double, longitude: Double) {"),
      code("    val updated = original.copy(title = title, description = description,"),
      code("                               latitude = latitude, longitude = longitude)"),
      code("    viewModelScope.launch {"),
      code("        _addState.value = UiState.Loading"),
      code("        val result = repository.updateEvent(updated)"),
      code("        if (result.isSuccess) _addState.value = UiState.Success(Unit)"),
      code("        else _addState.value = UiState.Error(result.exceptionOrNull()?.message ?: \"Erreur\")"),
      code("    }"),
      code("}"),
      h2("5.3  Ce que P3 doit maitriser"),
      bullet("osmdroid : MapView, Marker, Overlay, GeoPoint, Projection.toPixels()"),
      bullet("RecyclerView : Adapter, ViewHolder, LayoutManager, submitList"),
      bullet("Android PopupWindow : creation, positionnement avec getLocationInWindow()"),
      bullet("BottomNavigationView : setOnItemSelectedListener, gestion de la visibilite"),
      bullet("Parcelable : passer des objets complexes entre Activity via Intent (mode edition)"),
      bullet("Kotlin Flow : collect, catch - consommation dans lifecycleScope"),
      bullet("Event.copy() : modifier un objet immutable partiellement"),
      bullet("Gestion des permissions Android : ACCESS_FINE_LOCATION, requete runtime"),
      bullet("Material 3 : FloatingActionButton, MaterialToolbar, BottomNavigationView, popups"),
      pageBreak(),

      // COMMON
      h1("6. Package common \u2014 Utilitaires partages"),
      p("Chemin : app/src/main/java/fr/itii/geoevent_kotlin/common/", { italic: true }),
      p("Les classes du package common sont utilisees par P1, P2 et P3. Aucun membre n\u2019en est proprietaire exclusif."),
      fileTable([
        ["UiState.kt", "sealed class", "Etats d\u2019une operation async : Loading, Success<T>, Error - utilise par tous les ViewModels"],
        ["ViewModelFactory.kt", "class (generique)", "Factory generique T : ViewModel - une seule factory pour tous les ViewModels avec dependances"],
      ]),
      new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),
      h2("common/UiState.kt  \u2014  sealed class"),
      p("Le when(state) est exhaustif : le compilateur force a gerer tous les cas (Loading, Success, Error, null). Impossible d\u2019oublier un etat."),
      code("sealed class UiState<out T> {"),
      code("    object Loading : UiState<Nothing>()"),
      code("    data class Success<T>(val data: T) : UiState<T>()"),
      code("    data class Error(val message: String) : UiState<Nothing>()"),
      code("}"),
      new Paragraph({ spacing: { before: 160 }, children: [] }),
      h2("common/ViewModelFactory.kt  \u2014  factory generique"),
      p("Accepte un lambda creator : () -> T et instancie n\u2019importe quel ViewModel avec parametres. Remplace les factories specifiques qui existaient auparavant."),
      code("class ViewModelFactory<T : ViewModel>("),
      code("    private val creator: () -> T"),
      code(") : ViewModelProvider.Factory {"),
      code("    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T"),
      code("}"),
      p("Usage :"),
      code("val viewModel: MapViewModel by viewModels {"),
      code("    ViewModelFactory { MapViewModel(ServiceLocator.eventRepository) }"),
      code("}"),
      pageBreak(),

      // TRANSVERSAL
      h1("7. Connaissances transversales (P1 + P2 + P3)"),
      conceptTable([
        ["Android Activity", "Composant principal de l\u2019UI. Chaque ecran est une Activity avec un cycle de vie (onCreate, onResume, onPause, onDestroy)."],
        ["ViewModel", "Survit aux rotations d\u2019ecran. Stocke la logique metier. Ne detient jamais de reference vers une Activity."],
        ["ViewBinding", "Genere une classe binding depuis le layout XML. Acces type et null-safe aux vues."],
        ["by viewModels { }", "Delegue Android qui cree ou recupere le ViewModel lie a l\u2019Activity. Meme ViewModel retourne apres rotation."],
        ["StateFlow.collect { }", "Methode suspendue qui ecoute les emissions. Doit etre dans lifecycleScope. Se reveille a chaque changement d\u2019etat."],
        ["suspend fun", "Fonction suspendue sans bloquer le thread. Les appels Firestore utilisent .await() pour transformer un Task en suspend."],
        ["Material 3", "Systeme de design Google. Theme violet #7C57C9, arrondi 12/16/28dp. Composants : MaterialButton, TextInputLayout, MaterialCardView, FAB, BottomNavigationView."],
        ["Git Branching", "Travail sur feature/p2-map. Merge via Pull Request vers master avec revue de code."],
      ]),
      pageBreak(),

      // FLUX DE DONNEES
      h1("8. Flux de donnees complet"),
      h2("8.1  Lecture des evenements (temps reel)"),
      code("Firestore (cloud)"),
      code("  \u2514\u2500[addSnapshotListener]\u2500\u25BA p1/FirestoreDataSource.getEventsFlow()"),
      code("       \u2514\u2500[callbackFlow]\u2500\u25BA p1/FirestoreEventRepository.getEvents()"),
      code("            \u2514\u2500[Flow<List<Event>>]\u2500\u25BA p3/MapViewModel.loadEvents()"),
      code("                 \u2514\u2500[StateFlow eventsState]\u2500\u25BA p3/MainActivity.displayEvents()"),
      code("                      \u251C\u2500\u25BA mapService.addMarker() \u2192 p3/OsmMapService \u2192 Marker"),
      code("                      \u2514\u2500\u25BA eventAdapter.submitList() \u2192 RecyclerView onglet Liste"),
      h2("8.2  Creation d\u2019un evenement"),
      code("Utilisateur tape sur carte \u2500\u25BA p3/MainActivity.showMapPopup()"),
      code("  \u2514\u2500\u25BA p3/AddEventActivity (EXTRA_LAT, EXTRA_LON pre-remplis)"),
      code("       \u2514\u2500\u25BA p3/EventViewModel.addEvent()  // userId + authorEmail injectes depuis Firebase"),
      code("            \u2514\u2500\u25BA p1/EventRepository.addEvent()"),
      code("                 \u2514\u2500\u25BA p1/FirestoreDataSource \u2192 Firestore \u2192 snapshot \u2192 carte mise a jour"),
      h2("8.3  Modification d\u2019un evenement"),
      code("Utilisateur tape sur marqueur (proprietaire) \u2500\u25BA showMarkerPopup()"),
      code("  \u2514\u2500\u25BA btnEditMarker.setOnClickListener"),
      code("       \u2514\u2500\u25BA currentEvents.find { it.id == eventId }  // recupere l\u2019Event complet"),
      code("            \u2514\u2500\u25BA startActivity(AddEventActivity + EXTRA_EVENT)"),
      code("                 \u2514\u2500\u25BA p3/EventViewModel.updateEvent(original.copy(...))"),
      code("                      \u2514\u2500\u25BA p1/FirestoreDataSource.updateEvent(.set())"),
      code("                           \u2514\u2500\u25BA Firestore \u2192 snapshot \u2192 carte mise a jour"),
      h2("8.4  Suppression depuis la bulle marqueur"),
      code("Utilisateur tape sur marqueur (proprietaire) \u2500\u25BA showMarkerPopup()"),
      code("  \u2514\u2500\u25BA btnDeleteMarker.setOnClickListener"),
      code("       \u2514\u2500\u25BA p3/MapViewModel.deleteEvent(eventId)"),
      code("            \u2514\u2500\u25BA p1/EventRepository.deleteEvent()"),
      code("                 \u2514\u2500\u25BA Firestore \u2192 snapshot \u2192 marqueur retire de la carte"),
      h2("8.5  Navigation Carte \u2194 Liste"),
      code("Utilisateur selectionne onglet Liste \u2500\u25BA BottomNavigationView.setOnItemSelectedListener"),
      code("  \u2514\u2500\u25BA mapContainer GONE, searchCard GONE, fabAddEvent GONE"),
      code("       \u2514\u2500\u25BA rvEvents VISIBLE (deja peuple par eventAdapter)"),
      code("Utilisateur clique un evenement de la liste"),
      code("  \u2514\u2500\u25BA EventAdapter.onEventClick(event)"),
      code("       \u2514\u2500\u25BA bottomNav.selectedItemId = R.id.nav_map"),
      code("            \u2514\u2500\u25BA mapService.centerOn(event.latitude, event.longitude, 15.0)"),
    ]
  }]
});

Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync('GeoEvent_Documentation_Technique.docx', buffer);
  console.log('Fichier genere : GeoEvent_Documentation_Technique.docx');
}).catch(err => {
  console.error('Erreur :', err);
  process.exit(1);
});
