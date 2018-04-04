---
layout: deploy
title: Securing the access
---

By default, the API agent is open and can be accessed by any client.
However, it is possible to restrict the access to only authorised users.
There are usually two components when it comes to securing a server: authentication, i.e., who can access the server, and authorization, i.e., what actions an authenticated user can perform.
For now, Accio only supports authentication, which means authenticated users can always access all API methods.

* TOC
{:toc}

## Accio users

Accio does not manage users by itself, and assume they are externally managed.
Consequently, there is no API method to create or alter users.

Users have three attributes:
* A username, which uniquely identifies a user.
It usually is a login, e.g., `jdoe`, or a full name, e.g., `John Doe`, although no constraint is imposed on its form.
Usernames are always treated as opaque strings.
* An email address, which may be used to send notifications to the user.
It is not used to identify a user, only the username is used for that purpose.
* Groups, which allow to give permissions to a set of users (unused for now).

## Authentication strategies

The agent comes with flexible authentication strategies.
Several strategies may be configured at the same time, where the first strategy able to authenticate the user will short-circuit the others.
There is no guarantee in the order in which different strategies will run.
All authenticated users will be automatically affected to the `system:authenticated` group, in addition to any other groups they may belong to.

Users provide their credentials through Thrift's [`ClientId`(https://twitter.github.io/finagle/guide/Contexts.html#current-client-id) context], though this is an implementation detail.
Credentials are provided as an opaque string, that has to be interpreted by an authentication strategy.
They are generally best considered as a token, though their meaning may vary depending on the authentication strategy.

### Static file

Users may be provided by a static file.
This authentication strategy is enabled by passing the `-auth.static_file=/path/to/users` option to the agent.
This file is only read when the server starts, which means that the server has to be restarted for any further changes to be taken into account.

The configuration file follows a format loosely inspired by the venerable [`/etc/passwd` Unix file](https://en.wikipedia.org/wiki/Passwd#Password_file).
```
credentials:username:email:group1,group2
```

The first and second columns specify the credentials and the username (respectively) and are mandatory.
The thrift and fourth column may be left out or empty.
Multiple groups are specified separated by commas.

### Webhook

Users may be authenticated by the mean of a webhook.
It means that a Web service will be called for each authentication request, and this Web service will be in charge of deciding whether to allow the user.
This authentication strategy is enabled by passing the `-auth.webhook_config_file=/path/to/config` option to the agent.
This JSON file specifies the service to call.
Below is a sample configuration:

```json
{
  "server": "localhost:8080",
  "path": "/auth",
  "headers": {
    "X-Producer": "Accio"
  },
  "timeout": "2.seconds"
}
```

The object can have the following fields.

| Name | Type | Description |
|:-----|:-----|:------------|
| server | string | Address to the server to contact, specified as a [Finagle name](https://twitter.github.io/finagle/guide/Names.html). |
| path | string; optional | Path to the service. Defaults to `/`. |
| headers | object; optional | Additional headers to define on the HTTP request. They may override default headers (such as `Content-Type`). |
| timeout | string; optional | Timeout when hitting contacting the service. Defaults to `5.seconds`. |
{: .table .table-striped}

The service receives a POST request with a JSON-encoded body with the following format:

```json
{
  "credentials": "USER-PROVIDED CREDENTIALS"
}
```

The service is expected to reply with a JSON-encoded body.
An successful authentication would return a response similar to the following:

```json
{
  "authenticated": true,
  "user": {
    "name": "jdoe",
    "email": "jdoe@gmail.com",
    "groups:" ["group1", "group2"]
  }
}
```

The `user.email` and `user.groups` fields may be left out if they are not relevant.
The `user.name` field should always be specified.
In the case it is left out, the credentials will be used as a user name, but this is not likely what you want.

A rejected authentication would return the following response:

```json
{
  "authenticated": false
}
``` 

The authentication result provided by the server, whatever the outcome (accepted or defined), will be cached locally on the server, to avoid hitting the service too often.
The duration of this cache is configured by the `-auth.webhook_cache_ttl` option, and defaults to 2 minutes. 


### Trust everybody

As a convenience, it is possible to extract the user's identity directly from the provided credentials.
This authenticated strategy is enabled by passing the `-auth.trust` option to the agent.
With this strategy, the credentials are expected to follow the following format:
```
username:email:group1,group2
```

The second and third part may be left out or empty.
Multiple groups are specified separated by commas.

## Anonymous users

The agent agent may allow anonymous users, i.e., users that either do not provide any credentials or provide invalid credentials.
Such users will always have as username `system:anonymous` and belong to a single group `system:authenticated`.
Anonymous users are enabled or disabled with the `-auth.allow_anonymous` flag.
They are currently enabled by default.
