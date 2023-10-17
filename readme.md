[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/spectacular/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/spectacular/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/7b6mpF6Qcf)
<img src="/doc/images/github.png" valign="middle">

# Spectacular

__Typeclasses for rendering different values as text for different audiences__

The idea of a `Show` typeclass is well established as a better alternative to using `toString`, but it
is limited by its inability to distinguish different audiences. A fully-generic typeclass for
arbitrary different audiences is possible, but is usually better handled with different `Text` types
too. Spectacular's compromise of distinguishing with `Show` and `Debug` typeclasses will be sufficient
for most purposes, and has the distinction that `debug` will always provide _some_ `Text` value, while
`show` will require an appropriate `Show` instance to be provided.

## Features

- typeclass-based means of converting values to text
- defines the `Show` typeclass for text destined for end users
- defines the `Debug` typeclass for text destined for the developer
- extension methods `show` and `debug` will apply conversions using these typeclasses
- the `show` method may fail if a `Show` is not available
- the `debug` method will fall back to a `Show` method and then `toString` if no `Debug` instance exists
- `Debug` instances are generically derived for products and coproducts


## Availability

Spectacular has not yet been published as a binary.

## Getting Started

### `Show` and `Debug`

Given a value of some type, e.g. a value `artifact` of type `Artifact`, calling
`artifact.show` will try to resolve a `Show[Artifact]` typeclass and apply it
to `artifact`. If such a contextual instance exists in scope the result will be
a `Text` value; if it does not, then a compile error will be raised.

Calling `artifact.debug` works similarly, but will never fail to compile:
First, a `Debug[Artifact]` will be sought and applied. Failing that an
`Encoder[Artifact]` will be used (see below). Then, a `Show[Artifact]` will be
tried instead. Finally, if that fails, a `Text` value will be created from
`artifact.toString`. Since `toString` is always defined (however inadequately),
the call always succeeds.

Note that this only applies when calling `debug` on a value; summoning the
`Debug` instance and applying it directly will still fail to compile if no
`Debug` instance exists in scope.

Both `Show` and `Debug` provide very similar functionality, and for many types,
their implementations will be the same. They differ primarily in their intended
audience: `Show` instances should produce textual output which is intended for
_users_, not _programmers_, while `Debug` output is intended only for
programmers. The absence of a `Show` typeclass for a particular type
corresponds to that type not being intended for user consumption, while a
programmer should always be happy to see _some representation_ of a value,
however technical.

Thus, messages which are intended for end-users should use `Show` and
"internal" messages should use `Debug`.


### Defining `Show` and `Debug` instances

Creating a given instance of `Show` or `Debug` is simple in both cases. Both
are single-abstract-method traits, so definitions are often take just a single
line. For example,
```scala
given Debug[Short] = short => t"Short(${short.toString})"
```

Since the `debug` extension method will fall back to using a `Show` instance
whenever a `Debug` instance is not available, if the implementations are
identical, then it is sufficient just to provide a `Show` instance.

### Generic derivation

Every product type, such as case classes, will have a default `Debug` instance,
derived from calling `debug` on each of its parameters. This will exist even if
some parameters do not have their own `Debug` instance, since a `Show`
typeclass will be used as a fallback, and `toString` as a last resort.

For example, the case class,
```scala
class Id(id: String) { override def toString(): String = id }
case class Person(id: Id, name: Text, age: Int)
Person(Id("jqm"), t"Jennifer", 61).debug
```
will produce the text,
```
Person(id=jqm,name=t"Jennifer",age=61)
```
using the `toString` of `Id` and the `Debug` instances (provided by
Spectacular) for `Text` and `age`.  Note that `Text`'s `Debug` instance
produces pastable code, rather than simply returning the exact same `Text`
value, while its `Show` instance does exactly that.

### Showing `Boolean` values

The values `true` and `false` often mean different things in different
scenarios, and without specifying, Spectacular will not presume any particular
`show` serialization of a `Boolean` value.

But by importing a `BooleanStyle` value from the `spectacular.booleanStyles`
package, a style can be chosen, for example,
```scala
import booleanStyles.yesNo
Io.println(true.show)
```
will print the text `yes`.

Several `BooleanStyle` options are provided in the `booleanStyles` package,
 - `yesNo` - `yes` or `no`
 - `onOff` - `on` or `off`
 - `trueFalse` - `true` or `false`
 - `oneZero` - `1` (`true`) or `0` (`false`)
and it is trivial to provide alternatives, for example:
```scala
import gossamer.*
given posNeg: BooleanStyle = BooleanStyle(t"+", t"-")
```

### `Encoder` and `Decoder`

A further typeclass, `Encoder`, also converts from a particular type to `Text`,
but comes with a complementary `Decoder` typeclass and has a particular intent:
it is intended to represent a canonical way to encode a value as a string, such
that that text may be decoded to restore the original value.

For example, a `Url` (as defined in
[Nettlesome](https://github.com/propensive/nettlesome/)) represents the
structure of a URL, but is encoded in a very standard way to a familiar
representation of a URL, such as `https://example.com/`. This conversion should
be provided by an `Encoder` instance, and a corresponding `Decoder` should be
provided in order to parse the `Text` representation of the URL back into a
`Url` instance.

`Encoder`s and `Decoder`s are intended to be used by libraries which use text
as a serialization format. [Jacinta](https://github.com/propensive/jacinta/)
allows any type for which an `Encoder` exists to be serialized to JSON, and any
type for which a `Decoder` exists to be read from JSON.
[Xylophone](https://github.com/propensive/xylophone/) provides the same
functionality for XML and [Cellulose](https://github.com/propensive/cellulose/)
for CoDL.

#### Decoding errors

While encoding to text will normally succeed in all cases, it's common for
decoder (or deserialization) to fail, if the input text is in the wrong format.
However, the API of `Decoder` does not include any optionality in the signature
of its `decode` method. That's because _capabilities_ should be used to handle
failures, with greater flexibility.  Given `Decoder` instances should include
appropriate `using` clauses to demand the capability to raise errors. If using
[Perforate](https://github.com/propensive/perforate/) for error handling, that
implies a `Raises` instance, while Scala's checked exceptions require a
`CanThrow` instance for the exception type.



## Status

Spectacular is classified as __maturescent__. For reference, Scala One projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, are still ready to
be used, but caution should be taken if there is a mismatch between the
project's stability level and the importance of your own project.

Spectacular is designed to be _small_. Its entire source code currently consists
of 410 lines of code.

## Building

Spectacular can be built on Linux or Mac OS with [Fury](/propensive/fury), however
the approach to building is currently in a state of flux, and is likely to
change.

## Contributing

Contributors to Spectacular are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/spectacular/labels/beginner">beginner</a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Spectacular easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Spectacular was designed and developed by Jon Pretty, and commercial support and training is available from
[Propensive O&Uuml;](https://propensive.com/).



## Name

Something _spectacular_ relates to a _spectacle_ or _show_; in this case, the `Show` typeclass.

In general, Scala One project names are always chosen with some rationale, however it is usually
frivolous. Each name is chosen for more for its _uniqueness_ and _intrigue_ than its concision or
catchiness, and there is no bias towards names with positive or "nice" meanings—since many of the
libraries perform some quite unpleasant tasks.

Names should be English words, though many are obscure or archaic, and it should be noted how
willingly English adopts foreign words. Names are generally of Greek or Latin origin, and have
often arrived in English via a romance language.

## Logo



## License

Spectacular is copyright &copy; 2023 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
