package javaapplication1.Core;

public class DatabaseCore {

    // create variables for all 10 trees

    // create two variables for BinaryFile (exists in the proejct)
    // one for first main tree, with people, the other one with tests

    // in the constructor, take names (path) to the files and a bool param
    // that shows whether we want to start fresh (true) or restore state from these
    // files (false)

    // add a public method to close database which will call a saving to file method
    // on a tree,
    // passing it the coresponding path to file.

    // create all public operations presented as functions with return values
    // for where input like X is required, let the function take a parameter,
    // which will be passed from outside, from gui (dont care about ui for now)
    // also, remember this: During outputs of tests, system prints always also all
    // data about person,
    // to whom test was performed and during all outputs system prints also count of
    // printed data.",
    // so in function return values also include these details

    // don't use shortened variable names. write full names in cammel case.
    // for cases when function needs to return several types of data,
    // create a record(){} for it

    // create a child of /LinearHashing/Core/PCR.java to represent pcr with the
    // following variables:
    // • date and time of the test - long

    // • unique patient number – string

    // • unique random PCR test code – integer

    // • unique code of the workplace that performed the PCR test – integer

    // • district code – integer

    // • region code – integer

    // • test result – boolean

    // • test value – double

    // • note – string

    // in overriden methods like write and read from bytes, put into bytestream the
    // length of string,
    // and then the bytes representing the string, so that when turning it from the
    // bytes,
    // read the length first and then the string bytes itslelf. do the same with
    // pcrs

    // create a new class (not child) but similar to Person.java class to have these
    // member variables:
    // • first name – string

    // • last name – string

    // • date of birth - long

    // • unique patient number – string
}