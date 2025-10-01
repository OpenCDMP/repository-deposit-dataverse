# Repository Deposit Dataverse for OpenCDMP

**repository-deposit-dataverse** is an implementation of the `repository-deposit-base` package that enables the deposition of **OpenCDMP Plans** into the **Dataverse** repository. This service allows users of the **OpenCDMP** platform to submit their DMPs to Dataverse, minting a **Digital Object Identifier (DOI)** for each plan. The service is built using **Spring Boot** and can be easily integrated with OpenCDMP as a repository deposit option.

## Overview

The **Dataverse** repository is a widely-used open-access research repository that offers DOI assignment for uploaded content. By using the **repository-deposit-dataverse** service, OpenCDMP users can directly deposit their DMPs into Dataverse, making the plans citeable and publicly available. The service supports both **system-based** and **user-based** depositions depending on the configuration.

- **Deposits**: Supported for DMPs into the Dataverse repository.
- **DOI Minting**: Each successful deposition will mint a DOI through Dataverse.

## Features

- **Plan Deposits**: Deposit OpenCDMP plans into Dataverse.
- **DOI Minting**: Automatically mint DOIs for each submitted plan.
- **Spring Boot Microservice**: Built as a Spring Boot microservice for seamless integration with OpenCDMP.

## Key Endpoints

This service implements the following endpoints as per `DepositController`:

### Deposit Endpoint

- **POST `/deposit`**: Deposits a plan into Dataverse and returns the DOI.

```bash
POST /deposit
{
    "planDepositModel": { ... },
    "authToken": "user_oauth2_access_token"
}
```

### Configuration Endpoint

- **GET `/configuration`**: Returns the repository's configuration for Dataverse.

```bash
GET /configuration
```

### Logo Endpoint

- **GET `/logo`**: Returns the Dataverse logo in base64 format if available.

```bash
GET /logo
```

## Example

To deposit a plan into Dataverse and mint a DOI:

```bash
POST /deposit
{
    "planDepositModel": { ... },
    "authToken": "user_oauth2_access_token"
}
```

## License

This repository is licensed under the [EUPL 1.2 License](LICENSE).

## Contact

For questions or support regarding this implementation, please contact:

- **Email**: opencdmp at cite.gr
