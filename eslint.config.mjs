export default [
    {
        files: ["**/*.js"],
        languageOptions: {
            ecmaVersion: "latest",
            sourceType: "module",
        },
        rules: {
            "no-var": "error",
            "eqeqeq": "error",
            "no-undef": "error",
            "no-unused-vars": "warn",
            "no-unreachable": "error"
        }
    }
];