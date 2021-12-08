makeinfo quickjs.texi --docbook
pandoc -f docbook spec.xml -t gfm > quickjs.md
makeinfo jsbignum.texi --docbook
pandoc -f docbook jsbignum.xml -t gfm > jsbignum.md
