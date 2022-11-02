# Transformations

When a new sink `Processor` is requested using the endpoint `/api/smartevents_mgmt/v1/bridges/{id}/processors` you can optionally provide a `Transformation`
to modify the `Event` sent to the [Action](ACTIONS.md) defined on the `Processor`.

You can use a `Transformation` to construct an entirely new `Event`, or selectively pass through parts of the original `Event`
to your [Action](ACTIONS.md)

If you do not specify a `Transformation` then the `Event` is passed through unchanged to your [Action](ACTIONS.md).

## Writing a Transformation

`Transformations` are defined using the [Qute Templating](https://quarkus.io/guides/qute-reference) engine. You can provide a template to be used to transform the original `Event` as part of
your `Processor` definition:

```json
{
  "transformationTemplate" : "hello {data.name}"
}
```

In the above example, the original `Event` is transformed into the String `hello world` if the `Event` received by the `Processor`
has the following structure:

```json
{
  "data": {
    "name" : "world"
  }
}
```

