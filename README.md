# OpenCDMP Dataverse deposit service

**repository-deposit-dataverse** is an [OpenCDMP](https://github.com/OpenCDMP/OpenCDMP) deposit service that publishes plans to a [Dataverse](https://dataverse.org/) installation. It creates a dataset with the plan's citation metadata and files and returns the dataset's persistent identifier, typically a DOI, which OpenCDMP stores with the plan.

The service implements the [repository-deposit-base](https://github.com/OpenCDMP/repository-deposit-base) contract. See [Deposit services](https://opencdmp.github.io/optional-services/deposit-services/) in the OpenCDMP documentation.

## Features

- Deposits a plan as a new Dataverse dataset, or as a new version of a dataset deposited earlier
- Publishes under the organisation's account or the user's own account, with a Dataverse API token
- Uploads the plan document (PDF), the machine-actionable plan (RDA JSON) and the supporting files (ZIP)
- Publishes the dataset right away, or leaves it as a draft to publish in Dataverse, as the organisation chooses
- Lets users review and edit the citation metadata before it is published, with the controlled vocabularies of the target installation
- Reads the required fields and vocabularies from the target installation's own citation metadata block
- Fills Dataverse fields from the answers of the plan blueprint and asset templates, through [semantics](https://opencdmp.github.io/optional-services/deposit-services/dataverse-semantics/)

## Requirements

- Dataverse 6.9, and a Dataverse account with an API token and permission to create and publish datasets in the target Dataverse collection
- An OpenCDMP 2.0 installation, and its Keycloak realm with the `plugins` client scope
- To build from source: Java 21 and Maven. The service is a Spring Boot 4.0 application built on `repository-deposit-base`; the exact versions are in [`pom.xml`](pom.xml).

## Run with Docker

The image is [`opencdmp/repository-deposit-dataverse`](https://hub.docker.com/r/opencdmp/repository-deposit-dataverse) on Docker Hub. It listens on port `8080`, reads its settings from `/config/app-<PROFILE>.env` and writes its logs to `/logs`:

```bash
docker run -d --name opencdmp-dataverse \
  --network <opencdmp-network> \
  -e PROFILE=production \
  -v /srv/opencdmp/dataverse/config:/config \
  -v /srv/opencdmp/dataverse/logs:/logs \
  opencdmp/repository-deposit-dataverse:<tag>
```

Use the tag that the [changelog](https://opencdmp.github.io/deployment/install/changelog/) lists for your OpenCDMP release. Keep the port on the internal network: the `/actuator` endpoints are served without authentication. For a Docker Compose entry, see [Registering plugin services](https://opencdmp.github.io/deployment/configuration/plugin-services/#step-1-run-the-service).

## Build from source

```bash
mvn clean package
docker build -t repository-deposit-dataverse:local .
```

Maven builds `web/target/repository-deposit-web-1.0.0-SNAPSHOT.jar`, and the `Dockerfile` builds the service from source on its own. The service contacts Keycloak at `IDP_ISSUER_URI` when it starts, and does not start if it cannot reach it. To run the JAR against a local OpenCDMP installation, see [Run and test a plugin locally](https://opencdmp.github.io/developers/plugins/run-locally/).

## Configuration

The service reads its settings from environment variables or from `app-<PROFILE>.env`. The main ones:

| Variable | Description |
| --- | --- |
| `IDP_ISSUER_URI` | Keycloak realm URL, the same value as in the OpenCDMP API service |
| `IDP_CLAIMS_ROLES_PATH` | `plugins.roles` |
| `SECURITY_AUDIENCE` | `plugins` |
| `OPENCDMP_DOMAIN` | Address of the OpenCDMP installation, with a trailing slash |
| `DEPOSIT_DATAVERSE_URL` | Dataverse API URL, such as `https://dataverse.example.org/api/` |
| `DEPOSIT_DATAVERSE_RECORD_URL` | Address of a deposited dataset, with `{doi_id}` in place of its persistent identifier |
| `DEPOSIT_DATAVERSE_ACCESS_TOKEN` | API token of the installation's Dataverse account |
| `DEPOSIT_DATAVERSE_HOST` | Alias of the Dataverse collection that datasets are created in |
| `DEPOSIT_DATAVERSE_SUPPORTS_PREVIEW` | Metadata review before publishing (default `true`) |
| `STORAGE_PATH` | Folder of temporary and shared files |

All variables, with defaults and examples, are in [Deposit services: Dataverse](https://opencdmp.github.io/deployment/configuration/backend/deposit/#dataverse); the authentication, logging and storage settings are in [Settings common to all plugin services](https://opencdmp.github.io/deployment/configuration/plugin-services/#settings-common-to-all-plugin-services). Each organisation can override the account and the record defaults in its [organisation settings](https://opencdmp.github.io/optional-services/deposit-services/#organisation-settings).

### Publishing accounts

For each deposit, the service uses the first of these Dataverse tokens that is set:

1. The API token the user saved in their [profile](https://opencdmp.github.io/using/profile-settings/#repository-accounts) or typed in the publish dialog (`authInfo.authFields`, code `dataverse-access-token`)
2. The organisation's **Access token** setting
3. `DEPOSIT_DATAVERSE_ACCESS_TOKEN`

OpenCDMP decides which accounts a user may publish under, and never falls back to the organisation's or the installation's account when a user chose their own. See [Publishing accounts and files](https://opencdmp.github.io/optional-services/deposit-services/#publishing-accounts-and-files).

## Register with OpenCDMP

Register the service for the whole installation in the configuration of the API service, or for one organisation under **Settings > Plugins > Add plugin**. Use the plugin id `dataverse`, and register a PDF and an RDA JSON transformer with it. See [Registering plugin services](https://opencdmp.github.io/deployment/configuration/plugin-services/) and [Organisation plugins](https://opencdmp.github.io/administering/tenant-management/plugins/).

## API

All endpoints are under `/api/deposit`. They accept only a Bearer token from the installation's Keycloak realm that carries the `plugins` audience; the OpenCDMP API service obtains one for each call. See [Let the plugin accept the API service's tokens](https://opencdmp.github.io/deployment/configuration/plugin-services/#plugin-tokens).

| Endpoint | Description |
| --- | --- |
| `POST /api/deposit` | Deposit a plan and return the dataset's persistent identifier |
| `POST /api/deposit/preview` | Return the metadata that would be deposited, as an editable form |
| `GET /api/deposit/vocab/{fieldCode}?like=` | Search the vocabulary behind a review field |
| `GET /api/deposit/configuration` | Return the service's `DepositConfiguration` |
| `GET /api/deposit/logo` | Return the Dataverse logo, Base64-encoded |

A deposit takes a `PlanDepositModel`. For a new version of a dataset deposited earlier, `previousDOI` holds the dataset's persistent identifier:

```json
{
  "planModel": {
    "id": "<plan-id>",
    "label": "My research plan",
    "previousDOI": "<persistent-id-of-the-earlier-version>"
  },
  "authInfo": {
    "authFields": [
      { "code": "dataverse-access-token", "textValue": "<user-api-token>" }
    ]
  }
}
```

The models are described in the [Deposit plugin guide](https://opencdmp.github.io/developers/plugins/deposit/#key-interfaces-and-classes) and [Common models](https://opencdmp.github.io/developers/plugins/common-models/).

## Deposit preview

The service reports `supportsPreview: true` in its configuration unless `DEPOSIT_DATAVERSE_SUPPORTS_PREVIEW` is `false`. OpenCDMP then shows users the dataset's citation metadata in a form before anything is submitted to Dataverse:

- `POST /api/deposit/preview` takes the same `PlanDepositModel` as a deposit; `authInfo` is not needed. It runs the same mapping as a deposit and returns a `DepositPreviewModel`: the form, with the mapped values filled in. Overrides saved earlier, in the optional `storedFields` and `storedGroups`, are merged in.
- `POST /api/deposit` applies the user's `editedFields` (single values) and `editedGroups` (repeatable groups) to the citation metadata just before it is submitted.

The contract is described in [Deposit preview and metadata review](https://opencdmp.github.io/developers/plugins/deposit-preview/).

### The form

Fields are Mapped (filled in from the plan and editable, such as `title`, `subject` and `authorName`) or System (read-only, such as `files`, and `subject` when the organisation locks it). The Dataverse form has no Manual fields.

| Group code | Contents |
|---|---|
| `files` | `files` (read-only): the names of the files the deposit will upload, when the platform provides them |
| `basic-information` | `title`, `alternativeURL`, `dateOfDeposit`, `subtitle`, `notesText` |
| `authors` | repeatable rows: `authorName`, `authorAffiliation`, `authorIdentifierScheme` (vocabulary select) |
| `descriptions` | repeatable rows: `dsDescriptionValue` |
| `subjects` | `subject` (controlled-vocabulary tags, vocabulary autocomplete) |
| `contacts` | repeatable rows: `datasetContactEmail`, `datasetContactName`, `datasetContactAffiliation` |
| `contributors` | repeatable rows: `contributorType` (vocabulary select), `contributorName` |
| `funding` | repeatable rows: `grantNumberAgency`, `grantNumberValue` (Dataverse's compound field `grantNumber`) |
| `publications` | repeatable rows: `publicationRelationType` (vocabulary select), `publicationCitation`, `publicationIDType` (vocabulary select), `publicationIDNumber`, `publicationURL` |
| `keywords` | repeatable rows: `keywordTermURI`, `keywordVocabulary`, `keywordVocabularyURI`, `keywordValue` |
| `identifiers` | repeatable rows: `otherIdValue`, `otherIdAgency` (Dataverse's compound field `otherId`) |

Field codes are unique across the form, and the service ignores codes it does not know. An organisation can hide groups and lock record defaults in its settings.

### Required fields and vocabularies

Each Dataverse installation can customise its citation metadata block. On every preview and deposit, the service reads the installation's schema (`GET {repository-url}metadatablocks/citation`, cached for 60 minutes as `dataverseCitationSchema` in `cache.yml`) and:

- marks `title`, `author.authorName`, `dsDescription.dsDescriptionValue`, `subject` and `datasetContact.datasetContactEmail` `required` when the schema's `isRequired` flag says so. Other fields are never marked required;
- marks each group `required` from the top-level `isRequired` of its compound field;
- builds the options of the controlled-vocabulary fields (`subject`, `contributorType`, `publicationIDType`, `publicationRelationType`) from the schema's `controlledVocabularyValues`;
- leaves out a value built from the plan that is not in the installation's vocabulary, since Dataverse would reject the whole deposit with a 400 error.

On demo.dataverse.org, all five fields are required, and so are the groups `author`, `dsDescription` and `datasetContact`; `contributor`, `grantNumber`, `publication`, `keyword` and `otherId` are not. Another installation may require fewer, or none.

### How edits apply

- An edit to a single value replaces the mapped value, even when it is blank.
- An edited repeatable group replaces the whole list.
- Edits to the controlled-vocabulary fields are sent as the user made them; only the values built from the plan are checked against the schema. The form offers the installation's values for these fields.
- Saved overrides are merged into the preview by `DepositPreviewMergeHelper` from repository-deposit-base, with the [shared merge rules](https://opencdmp.github.io/developers/plugins/deposit-preview/#depositpreviewmergehelper).

### Vocabulary search

`GET /api/deposit/vocab/{fieldCode}?like=<term>` returns autocomplete options for the review fields marked `vocabSearch`: `subject`, `publicationRelationType` and `publicationIDType`. The options are filtered from the cached citation schema; any other code returns an empty list.

## Related resources

- [Deposit services overview](https://opencdmp.github.io/optional-services/deposit-services/)
- [Dataverse semantics reference](https://opencdmp.github.io/optional-services/deposit-services/dataverse-semantics/)
- [Deposit services configuration](https://opencdmp.github.io/deployment/configuration/backend/deposit/#dataverse)
- [Registering plugin services](https://opencdmp.github.io/deployment/configuration/plugin-services/)
- [Deposit a plan](https://opencdmp.github.io/using/plans/deposit-a-plan/): the user guide
- [Deposit plugin guide](https://opencdmp.github.io/developers/plugins/deposit/): building your own deposit service

## Contributing

Report bugs and suggest changes in [GitHub Issues](https://github.com/OpenCDMP/repository-deposit-dataverse/issues). See [Contributing](https://opencdmp.github.io/developers/contributing/) for how to propose a change.

## License

repository-deposit-dataverse is licensed under the [European Union Public Licence (EUPL) v1.2](LICENSE).

## Contact

Questions: email opencdmp at cite.gr.
