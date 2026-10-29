<script setup>
</script>
<template>
    <div class="SearchBar">
        <font-awesome-icon id="search" icon="magnifying-glass"></font-awesome-icon>
        <input type="text" spellcheck="false">
    </div>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght@6..144,1..1000&family=Quicksand:wght@300..700&display=swap');
.SearchBar{
    font-family:"Quicksand", sans-serif; 
    width: 100%;
    font-size: large;
    color:#b4bccb;
    display: flex;
    justify-content: center;
    border-radius: 1.25rem;
    align-items: center;
    align-self: center;
    justify-self: center;
    justify-content: center;
    border-radius: 1.2rem;
    border:1px solid #e6eaf2;
    background-color: #f8f9fd;
    justify-items: center;
    align-items: center;
}
.SearchBar:focus-within{
    border:1px solid #4b7bec;;
}
.SearchBar input{
    width: 100%;
    border: none;
    font-family:"Quicksand", sans-serif;
    padding:5px 10px;
    background-color: #f8f9fd;
    height:4vh;
    border-radius: 1.1rem;
    
}
.SearchBar input:focus{
    outline: none;
}
#search{
    margin-left: 10px;
}
</style>